import Foundation
import SwiftUI
import UIKit
import UserNotifications

#if canImport(AlarmKit)
import AlarmKit
#endif

final class WakeAppNotificationDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    static let alarmCategoryIdentifier = "wakeapp.alarm.category"
    static let snoozeActionIdentifier = "wakeapp.alarm.action.snooze"
    static let stopActionIdentifier = "wakeapp.alarm.action.stop"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        Self.installNotificationCategories()
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        NSLog("WakeApp: foreground notification will be presented id=\(notification.request.identifier)")
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .list, .sound, .badge])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        NSLog("WakeApp: notification interaction received id=\(response.notification.request.identifier)")
        Task {
            await WakeAppAlarmEngine.shared.handleNotificationResponse(response)
            completionHandler()
        }
    }

    static func installNotificationCategories() {
        UNUserNotificationCenter.current().setNotificationCategories([alarmCategory])
    }

    private static var alarmCategory: UNNotificationCategory {
        let snoozeAction = UNNotificationAction(
            identifier: snoozeActionIdentifier,
            title: "Snooze",
            options: [.foreground]
        )
        let stopAction = UNNotificationAction(
            identifier: stopActionIdentifier,
            title: "Stop",
            options: [.destructive, .foreground]
        )
        return UNNotificationCategory(
            identifier: alarmCategoryIdentifier,
            actions: [snoozeAction, stopAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(WakeAppNotificationDelegate.self)
    private var notificationDelegate

    init() {
        WakeAppNotificationDelegate.installNotificationCategories()
        WakeAppAlarmEngineBridge.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

@objc(WakeAppAlarmEngineBridge)
public final class WakeAppAlarmEngineBridge: NSObject {
    @objc public static let shared = WakeAppAlarmEngineBridge()

    private var didStart = false
    private var observerTokens: [NSObjectProtocol] = []

    @objc public func start() {
        guard !didStart else { return }
        didStart = true
        NSLog("WakeApp: starting native iOS alarm engine bridge.")

        let center = NotificationCenter.default

        observerTokens.append(
            center.addObserver(
                forName: .wakeAppAlarmSyncRequested,
                object: nil,
                queue: nil
            ) { _ in
                NSLog("WakeApp: received KMP alarm sync request.")
                Task {
                    await WakeAppAlarmEngine.shared.syncFromStoredPlans()
                }
            }
        )

        observerTokens.append(
            center.addObserver(
                forName: UIApplication.willEnterForegroundNotification,
                object: nil,
                queue: nil
            ) { _ in
                NSLog("WakeApp: app foregrounded, syncing alarm engine.")
                Task {
                    await WakeAppAlarmEngine.shared.syncFromStoredPlans()
                }
            }
        )

        Task {
            await WakeAppAlarmEngine.shared.syncFromStoredPlans()
        }
    }
}

private extension Notification.Name {
    static let wakeAppAlarmSyncRequested = Notification.Name("wakeapp.alarm.sync.requested")
}

private actor WakeAppAlarmEngine {
    static let shared = WakeAppAlarmEngine()

    private let defaults = UserDefaults.standard
    private let notificationCenter = UNUserNotificationCenter.current()
    private let calendar = Calendar.autoupdatingCurrent

    private let planStorageKey = "interval_plans"
    private let requestPrefix = "wakeapp_interval"
    private let alarmKitIDMapKey = "wakeapp.alarmkit.id.map"
    private let maxPendingNotifications = 64
    private let maxQueueDays = 7

    func handleNotificationResponse(_ response: UNNotificationResponse) async {
        let requestID = response.notification.request.identifier
        notificationCenter.removeDeliveredNotifications(withIdentifiers: [requestID])
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [requestID])

        switch response.actionIdentifier {
        case WakeAppNotificationDelegate.snoozeActionIdentifier:
            let snoozeMinutes = extractSnoozeMinutes(from: response.notification.request.content.userInfo)
            await scheduleSnoozedNotification(
                originalContent: response.notification.request.content,
                snoozeMinutes: snoozeMinutes
            )
            NSLog("WakeApp: snoozed notification id=\(requestID) by \(snoozeMinutes) minutes.")
        case WakeAppNotificationDelegate.stopActionIdentifier:
            NSLog("WakeApp: stopped notification id=\(requestID).")
        default:
            break
        }
    }

    func syncFromStoredPlans() async {
        let activePlans = loadPlans().filter {
            $0.isEnabled && !$0.activeDays.isEmpty && $0.intervalMinutes > 0
        }
        NSLog("WakeApp: native engine syncing \(activePlans.count) active plans.")

#if canImport(AlarmKit)
        if #available(iOS 26.0, *), await syncWithAlarmKitIfPossible(plans: activePlans) {
            NSLog("WakeApp: AlarmKit scheduling succeeded. Clearing fallback notifications.")
            await clearWakeNotifications()
            return
        }

        if #available(iOS 26.0, *) {
            await cancelAllAlarmKitAlarms()
        }
#endif

        NSLog("WakeApp: using notification fallback scheduler.")
        await syncWithNotifications(plans: activePlans)
    }

#if canImport(AlarmKit)
    @available(iOS 26.0, *)
    private func syncWithAlarmKitIfPossible(plans: [StoredIntervalPlan]) async -> Bool {
        guard isAlarmKitAvailableAtRuntime else { return false }

        do {
            try await AlarmManager.shared.requestAuthorization()
            if plans.isEmpty {
                await cancelAllAlarmKitAlarms()
                return true
            }

            var idMap = loadAlarmKitIDMap()
            let scheduleItems = buildAlarmKitScheduleItems(plans: plans)
            let desiredKeys = Set(scheduleItems.map(\.key))

            let obsoleteKeys = Set(idMap.keys).subtracting(desiredKeys)
            for key in obsoleteKeys {
                if let idString = idMap[key], let uuid = UUID(uuidString: idString) {
                    try? await AlarmManager.shared.cancel(id: uuid)
                }
                idMap.removeValue(forKey: key)
            }

            for item in scheduleItems {
                let alarmID = resolveAlarmKitID(key: item.key, idMap: &idMap)
                try await scheduleAlarmKitAlarm(id: alarmID, item: item)
            }
            NSLog("WakeApp: AlarmKit scheduled \(scheduleItems.count) recurring alarms.")

            saveAlarmKitIDMap(idMap)
            return true
        } catch {
            NSLog("WakeApp: AlarmKit scheduling failed: \(error.localizedDescription)")
            return false
        }
    }

    @available(iOS 26.0, *)
    private func cancelAllAlarmKitAlarms() async {
        let idMap = loadAlarmKitIDMap()
        if idMap.isEmpty { return }

        for (_, idString) in idMap {
            guard let alarmID = UUID(uuidString: idString) else { continue }
            try? await AlarmManager.shared.cancel(id: alarmID)
        }

        defaults.removeObject(forKey: alarmKitIDMapKey)
    }

    @available(iOS 26.0, *)
    private func scheduleAlarmKitAlarm(
        id: UUID,
        item: AlarmKitScheduleItem
    ) async throws {
        let recurrence = Alarm.Schedule.Relative.Recurrence.weekly(item.weekdays.toAlarmKitWeekdays())
        let relativeTime = Alarm.Schedule.Relative.Time(hour: item.time.hour, minute: item.time.minute)
        let schedule = Alarm.Schedule.Relative(time: relativeTime, repeats: recurrence)

        let stopButton = AlarmButton(
            text: "Dismiss",
            textColor: .white,
            systemImageName: "stop.circle.fill"
        )
        let presentationAlert = AlarmPresentation.Alert(
            title: "WakeApp",
            stopButton: stopButton
        )
        let presentation = AlarmPresentation(alert: presentationAlert)
        let metadata = WakeAlarmMetadata(
            planID: item.planID,
            hour: item.time.hour,
            minute: item.time.minute
        )
        let attributes = AlarmAttributes<WakeAlarmMetadata>(
            presentation: presentation,
            metadata: metadata,
            tintColor: .blue
        )
        let configuration = AlarmManager.AlarmConfiguration<WakeAlarmMetadata>(
            schedule: schedule,
            attributes: attributes
        )

        try await AlarmManager.shared.schedule(id: id, configuration: configuration)
    }

    @available(iOS 26.0, *)
    private var isAlarmKitAvailableAtRuntime: Bool {
        NSClassFromString("AlarmKit.AlarmManager") != nil || NSClassFromString("AKAlarmManager") != nil
    }

    @available(iOS 26.0, *)
    private func buildAlarmKitScheduleItems(plans: [StoredIntervalPlan]) -> [AlarmKitScheduleItem] {
        plans.flatMap { plan in
            generateTimes(
                start: plan.startTime,
                end: plan.endTime,
                intervalMinutes: plan.intervalMinutes
            )
            .map { time in
                AlarmKitScheduleItem(
                    key: "\(plan.id)|\(time.hour)|\(time.minute)",
                    planID: plan.id,
                    time: time,
                    weekdays: plan.activeDays
                )
            }
        }
    }

    private func loadAlarmKitIDMap() -> [String: String] {
        defaults.dictionary(forKey: alarmKitIDMapKey) as? [String: String] ?? [:]
    }

    private func saveAlarmKitIDMap(_ map: [String: String]) {
        defaults.set(map, forKey: alarmKitIDMapKey)
    }

    private func resolveAlarmKitID(key: String, idMap: inout [String: String]) -> UUID {
        if let existing = idMap[key], let parsed = UUID(uuidString: existing) {
            return parsed
        }

        let created = UUID()
        idMap[key] = created.uuidString
        return created
    }
#endif

    private func syncWithNotifications(plans: [StoredIntervalPlan]) async {
        if plans.isEmpty {
            await clearWakeNotifications()
            return
        }

        guard await ensureNotificationPermission() else {
            NSLog("WakeApp: notification permission denied, skipping fallback scheduling.")
            return
        }

        let now = Date()
        let candidates = buildNotificationCandidates(
            plans: plans,
            now: now
        )
        let queued = selectQueueWindow(candidates)
        NSLog("WakeApp: fallback queue picked \(queued.count) notifications from \(candidates.count) candidates.")

        await clearWakeNotifications()

        for occurrence in queued {
            await enqueueNotification(occurrence)
        }
    }

    private func ensureNotificationPermission() async -> Bool {
        let settings = await currentNotificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .denied:
            return false
        default:
            return await requestNotificationAuthorization()
        }
    }

    private func currentNotificationSettings() async -> UNNotificationSettings {
        await withCheckedContinuation { continuation in
            notificationCenter.getNotificationSettings { settings in
                continuation.resume(returning: settings)
            }
        }
    }

    private func requestNotificationAuthorization() async -> Bool {
        await withCheckedContinuation { continuation in
            notificationCenter.requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
                continuation.resume(returning: granted)
            }
        }
    }

    private func clearWakeNotifications() async {
        let pendingRequests = await pendingWakeNotificationRequests()
        let identifiers = pendingRequests.map(\.identifier)
        if !identifiers.isEmpty {
            notificationCenter.removePendingNotificationRequests(withIdentifiers: identifiers)
        }
    }

    private func pendingWakeNotificationRequests() async -> [UNNotificationRequest] {
        await withCheckedContinuation { continuation in
            notificationCenter.getPendingNotificationRequests { requests in
                continuation.resume(returning: requests.filter { $0.identifier.hasPrefix(self.requestPrefix) })
            }
        }
    }

    private func enqueueNotification(_ occurrence: QueuedNotificationOccurrence) async {
        let content = UNMutableNotificationContent()
        content.title = "WakeApp Alarm"
        content.body = "Interval alarm at \(occurrence.time.uiLabel)"
        content.sound = .default
        content.categoryIdentifier = WakeAppNotificationDelegate.alarmCategoryIdentifier
        content.userInfo = [
            UserInfoKeys.planID: occurrence.planID,
            UserInfoKeys.hour: occurrence.time.hour,
            UserInfoKeys.minute: occurrence.time.minute,
            UserInfoKeys.snoozeMinutes: occurrence.snoozeMinutes
        ]
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .timeSensitive
            content.relevanceScore = 1
        }
        NSLog(
            "WakeApp: enqueue request id=\(occurrence.requestID) " +
                "category=\(content.categoryIdentifier) snooze=\(occurrence.snoozeMinutes)m"
        )

        let dateComponents = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: occurrence.triggerDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: false)
        let request = UNNotificationRequest(
            identifier: occurrence.requestID,
            content: content,
            trigger: trigger
        )

        _ = await withCheckedContinuation { continuation in
            notificationCenter.add(request) { error in
                if let error {
                    NSLog("WakeApp: failed to enqueue fallback notification: \(error.localizedDescription)")
                }
                continuation.resume(returning: ())
            }
        }
    }

    private func buildNotificationCandidates(
        plans: [StoredIntervalPlan],
        now: Date
    ) -> [QueuedNotificationOccurrence] {
        let nowMinutesOfDay = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)

        var candidates: [QueuedNotificationOccurrence] = []

        for plan in plans {
            let times = generateTimes(
                start: plan.startTime,
                end: plan.endTime,
                intervalMinutes: plan.intervalMinutes
            )
            guard !times.isEmpty else { continue }

            for dayOffset in 0..<maxQueueDays {
                guard let dayDate = calendar.date(byAdding: .day, value: dayOffset, to: now) else { continue }
                guard let weekday = StoredWeekday(calendarWeekday: calendar.component(.weekday, from: dayDate)) else { continue }
                guard plan.activeDays.contains(weekday) else { continue }

                for time in times {
                    if dayOffset == 0, time.minutesOfDay <= nowMinutesOfDay { continue }

                    guard let triggerDate = calendar.date(
                        bySettingHour: time.hour,
                        minute: time.minute,
                        second: 0,
                        of: dayDate
                    ) else { continue }

                    candidates.append(
                        QueuedNotificationOccurrence(
                            planID: plan.id,
                            time: time,
                            dayOffset: dayOffset,
                            triggerDate: triggerDate,
                            snoozeMinutes: plan.snoozeMinutes
                        )
                    )
                }
            }
        }

        return candidates.sorted { $0.triggerDate < $1.triggerDate }
    }

    private func selectQueueWindow(
        _ candidates: [QueuedNotificationOccurrence]
    ) -> [QueuedNotificationOccurrence] {
        guard !candidates.isEmpty else { return [] }

        var selectedDays = 1
        for dayCount in 1...maxQueueDays {
            let count = candidates.filter { $0.dayOffset < dayCount }.count
            if count <= maxPendingNotifications {
                selectedDays = dayCount
            } else {
                break
            }
        }

        let scoped = candidates.filter { $0.dayOffset < selectedDays }
        if scoped.count <= maxPendingNotifications {
            return scoped
        }
        return Array(scoped.prefix(maxPendingNotifications))
    }

    private func loadPlans() -> [StoredIntervalPlan] {
        guard
            let encoded = defaults.string(forKey: planStorageKey),
            !encoded.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            let data = encoded.data(using: .utf8)
        else {
            NSLog("WakeApp: no persisted plans found.")
            return []
        }

        do {
            let decoded = try JSONDecoder().decode([StoredIntervalPlan].self, from: data)
            NSLog("WakeApp: loaded \(decoded.count) persisted plans.")
            return decoded
        } catch {
            let preview = String(encoded.prefix(220))
            NSLog("WakeApp: failed to decode stored plans: \(error.localizedDescription). payload=\(preview)")
            return []
        }
    }

    private func generateTimes(
        start: StoredTimeOfDay,
        end: StoredTimeOfDay,
        intervalMinutes: Int
    ) -> [StoredTimeOfDay] {
        guard intervalMinutes > 0 else { return [] }
        let startMinutes = start.minutesOfDay
        let endMinutes = end.minutesOfDay
        guard startMinutes <= endMinutes else { return [] }

        var result: [StoredTimeOfDay] = []
        var current = startMinutes
        while current <= endMinutes {
            result.append(StoredTimeOfDay(minutesOfDay: current))
            current += intervalMinutes
        }
        return result
    }

    private func extractSnoozeMinutes(from userInfo: [AnyHashable: Any]) -> Int {
        if let value = userInfo[UserInfoKeys.snoozeMinutes] as? Int {
            return max(1, value)
        }
        if let value = userInfo[UserInfoKeys.snoozeMinutes] as? NSNumber {
            return max(1, value.intValue)
        }
        return defaultSnoozeMinutes
    }

    private func scheduleSnoozedNotification(
        originalContent: UNNotificationContent,
        snoozeMinutes: Int
    ) async {
        let content = (originalContent.mutableCopy() as? UNMutableNotificationContent)
            ?? UNMutableNotificationContent()
        content.title = originalContent.title
        content.body = originalContent.body
        content.sound = .default
        content.categoryIdentifier = WakeAppNotificationDelegate.alarmCategoryIdentifier
        var updatedUserInfo = originalContent.userInfo
        updatedUserInfo[UserInfoKeys.snoozeMinutes] = snoozeMinutes
        content.userInfo = updatedUserInfo
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .timeSensitive
            content.relevanceScore = 1
        }

        let triggerDate = Date().addingTimeInterval(TimeInterval(snoozeMinutes * 60))
        let dateComponents = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: triggerDate
        )
        let trigger = UNCalendarNotificationTrigger(
            dateMatching: dateComponents,
            repeats: false
        )
        let request = UNNotificationRequest(
            identifier: "\(requestPrefix)_snooze_\(UUID().uuidString)",
            content: content,
            trigger: trigger
        )

        _ = await withCheckedContinuation { continuation in
            notificationCenter.add(request) { error in
                if let error {
                    NSLog("WakeApp: failed to schedule snooze notification: \(error.localizedDescription)")
                }
                continuation.resume(returning: ())
            }
        }
    }
}

private struct StoredIntervalPlan: Decodable {
    let id: String
    let startTime: StoredTimeOfDay
    let endTime: StoredTimeOfDay
    let intervalMinutes: Int
    let activeDays: Set<StoredWeekday>
    let isEnabled: Bool
    let snoozeMinutes: Int

    enum CodingKeys: String, CodingKey {
        case id
        case startTime
        case endTime
        case intervalMinutes
        case activeDays
        case isEnabled
        case snoozeMinutes
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        startTime = try container.decode(StoredTimeOfDay.self, forKey: .startTime)
        endTime = try container.decode(StoredTimeOfDay.self, forKey: .endTime)
        intervalMinutes = try container.decodeIfPresent(Int.self, forKey: .intervalMinutes) ?? 5
        activeDays = try container.decodeIfPresent(Set<StoredWeekday>.self, forKey: .activeDays)
            ?? Set(StoredWeekday.allCases)
        isEnabled = try container.decodeIfPresent(Bool.self, forKey: .isEnabled) ?? true
        snoozeMinutes = try container.decodeIfPresent(Int.self, forKey: .snoozeMinutes) ?? 5
    }
}

private struct StoredTimeOfDay: Codable, Hashable {
    let hour: Int
    let minute: Int

    init(hour: Int, minute: Int) {
        self.hour = hour
        self.minute = minute
    }

    init(minutesOfDay: Int) {
        self.hour = minutesOfDay / 60
        self.minute = minutesOfDay % 60
    }

    var minutesOfDay: Int {
        hour * 60 + minute
    }

    var uiLabel: String {
        let hour12 = hour % 12 == 0 ? 12 : hour % 12
        return String(format: "%d:%02d %@", hour12, minute, hour < 12 ? "AM" : "PM")
    }
}

private enum StoredWeekday: String, Codable, CaseIterable, Hashable {
    case Monday
    case Tuesday
    case Wednesday
    case Thursday
    case Friday
    case Saturday
    case Sunday

    init?(calendarWeekday: Int) {
        switch calendarWeekday {
        case 1: self = .Sunday
        case 2: self = .Monday
        case 3: self = .Tuesday
        case 4: self = .Wednesday
        case 5: self = .Thursday
        case 6: self = .Friday
        case 7: self = .Saturday
        default: return nil
        }
    }
}

private struct QueuedNotificationOccurrence {
    let planID: String
    let time: StoredTimeOfDay
    let dayOffset: Int
    let triggerDate: Date
    let snoozeMinutes: Int

    var requestID: String {
        let hourLabel = String(format: "%02d", time.hour)
        let minuteLabel = String(format: "%02d", time.minute)
        return "wakeapp_interval_\(planID)_\(dayOffset)_\(hourLabel)\(minuteLabel)"
    }
}

private enum UserInfoKeys {
    static let planID = "planId"
    static let hour = "hour"
    static let minute = "minute"
    static let snoozeMinutes = "snoozeMinutes"
}

private let defaultSnoozeMinutes = 5

#if canImport(AlarmKit)
@available(iOS 26.0, *)
private struct AlarmKitScheduleItem {
    let key: String
    let planID: String
    let time: StoredTimeOfDay
    let weekdays: Set<StoredWeekday>
}

@available(iOS 26.0, *)
private struct WakeAlarmMetadata: AlarmMetadata, Codable, Hashable, Sendable {
    let planID: String
    let hour: Int
    let minute: Int
}

@available(iOS 26.0, *)
private extension Set where Element == StoredWeekday {
    func toAlarmKitWeekdays() -> [Alarm.Schedule.Relative.Recurrence.Weekday] {
        self.compactMap { weekday in
            switch weekday {
            case .Monday: .monday
            case .Tuesday: .tuesday
            case .Wednesday: .wednesday
            case .Thursday: .thursday
            case .Friday: .friday
            case .Saturday: .saturday
            case .Sunday: .sunday
            }
        }
    }
}
#endif
