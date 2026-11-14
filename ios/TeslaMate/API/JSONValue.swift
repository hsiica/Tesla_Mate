import Foundation

/// Tolerant JSON tree. TeslaMateApi adds fields between releases and encodes SQL
/// nulls as JSON null. Call sites read only the keys they understand.
enum JSONValue: Decodable, Equatable, Sendable {
    case null
    case bool(Bool)
    case number(Double)
    case string(String)
    case array([JSONValue])
    case object([String: JSONValue])

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
            return
        }
        if let value = try? container.decode(Bool.self) {
            self = .bool(value)
            return
        }
        if let value = try? container.decode(Double.self) {
            self = .number(value)
            return
        }
        if let value = try? container.decode(String.self) {
            self = .string(value)
            return
        }
        if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
            return
        }
        if let value = try? container.decode([String: JSONValue].self) {
            self = .object(value)
            return
        }
        throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported JSON value")
    }

    subscript(key: String) -> JSONValue? {
        guard case .object(let object) = self else { return nil }
        return object[key]
    }

    func object(_ key: String) -> JSONValue {
        guard let value = self[key], case .object = value else { return .object([:]) }
        return value
    }

    func array(_ key: String) -> [JSONValue] {
        guard let value = self[key], case .array(let items) = value else { return [] }
        return items
    }

    /// Keyed object, or nil when the key is missing or JSON null.
    func objectIfPresent(_ key: String) -> JSONValue? {
        guard let value = self[key] else { return nil }
        if case .null = value { return nil }
        guard case .object = value else { return nil }
        return value
    }

    func string(_ key: String) -> String {
        guard let value = self[key] else { return "" }
        if case .string(let text) = value { return text }
        return ""
    }

    func int(_ key: String) -> Int {
        self[key]?.intValue ?? 0
    }

    func double(_ key: String) -> Double {
        self[key]?.doubleValue ?? 0
    }

    func bool(_ key: String) -> Bool {
        self[key]?.boolValue ?? false
    }

    /// Missing and JSON null stay nil. Used for drive energy and temperatures,
    /// which TeslaMateApi encodes with pointers.
    func optionalDouble(_ key: String) -> Double? {
        guard let value = self[key] else { return nil }
        if case .null = value { return nil }
        return value.doubleValue
    }

    var intValue: Int? {
        switch self {
        case .number(let number):
            guard number.isFinite, abs(number) < Double(Int.max) else { return nil }
            return Int(number.rounded(.towardZero))
        case .string(let text):
            if let value = Int(text) { return value }
            if let value = Double(text), value.isFinite, abs(value) < Double(Int.max) {
                return Int(value.rounded(.towardZero))
            }
            return nil
        default:
            return nil
        }
    }

    var doubleValue: Double? {
        switch self {
        case .number(let number):
            return number.isFinite ? number : nil
        case .string(let text):
            return Double(text)
        default:
            return nil
        }
    }

    var boolValue: Bool? {
        switch self {
        case .bool(let value):
            return value
        case .number(let number):
            return number != 0
        case .string(let text):
            switch text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
            case "true", "1", "yes":
                return true
            case "false", "0", "no":
                return false
            default:
                return nil
            }
        default:
            return nil
        }
    }
}
