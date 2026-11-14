import Foundation

func formatTeslaModel(_ model: String) -> String {
    let trimmed = model.trimmingCharacters(in: .whitespacesAndNewlines)
    switch trimmed.lowercased() {
    case "s", "3", "x", "y":
        return "Model \(trimmed.uppercased())"
    case "cybertruck":
        return "Cybertruck"
    case "semi":
        return "Semi"
    case "roadster":
        return "Roadster"
    default:
        return trimmed
    }
}

func formatFixed(_ value: Double, decimals: Int) -> String {
    let formatter = NumberFormatter()
    formatter.locale = .current
    formatter.minimumFractionDigits = decimals
    formatter.maximumFractionDigits = decimals
    formatter.numberStyle = .decimal
    formatter.usesGroupingSeparator = false
    return formatter.string(from: NSNumber(value: value)) ?? String(format: "%.\(decimals)f", value)
}

func formatQuantity(_ value: Double, decimals: Int, unit: String) -> String {
    "\(formatFixed(value, decimals: decimals)) \(unit)"
}

func formatSpeed(_ value: Double, units: Units) -> String {
    let decimals = value.rounded() == value ? 0 : 1
    return formatQuantity(value, decimals: decimals, unit: units.speedLabel)
}

func formatAPIDate(_ raw: String) -> String? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.isEmpty { return nil }
    guard let date = parseAPIDate(trimmed) else { return trimmed }
    let year = Calendar(identifier: .gregorian).component(.year, from: date)
    if year < 2000 { return nil }
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "yyyy-MM-dd HH:mm"
    formatter.timeZone = .current
    return formatter.string(from: date)
}

func parseAPIDate(_ raw: String) -> Date? {
    let fractional = ISO8601DateFormatter()
    fractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = fractional.date(from: raw) { return date }
    let basic = ISO8601DateFormatter()
    basic.formatOptions = [.withInternetDateTime]
    return basic.date(from: raw)
}

func formatDuration(_ durationStr: String, minutes: Int) -> String? {
    let trimmed = durationStr.trimmingCharacters(in: .whitespacesAndNewlines)
    if !trimmed.isEmpty { return trimmed }
    if minutes <= 0 { return nil }
    return String(format: "%02d:%02d", minutes / 60, minutes % 60)
}

struct HourMinute: Equatable, Sendable {
    var hours: Int
    var minutes: Int
}

func splitHours(_ hours: Double) -> HourMinute? {
    if hours <= 0 { return nil }
    let totalMinutes = max(1, Int((hours * 60).rounded()))
    return HourMinute(hours: totalMinutes / 60, minutes: totalMinutes % 60)
}

func hasCoordinates(_ latitude: Double, _ longitude: Double) -> Bool {
    latitude != 0 || longitude != 0
}

func formatCoordinates(_ latitude: Double, _ longitude: Double) -> String? {
    guard hasCoordinates(latitude, longitude) else { return nil }
    return "\(formatFixed(latitude, decimals: 5)), \(formatFixed(longitude, decimals: 5))"
}
