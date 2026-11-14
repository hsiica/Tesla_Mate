import Foundation
import Observation

@MainActor
@Observable
final class SettingsStore {
    static let urlKey = "teslamate.baseURL"
    static let carKey = "teslamate.selectedCarID"

    private let defaults: UserDefaults

    var baseURL: String {
        didSet { defaults.set(baseURL, forKey: Self.urlKey) }
    }

    var token: String {
        didSet { KeychainToken.write(token) }
    }

    var selectedCarID: Int? {
        didSet {
            if let selectedCarID {
                defaults.set(selectedCarID, forKey: Self.carKey)
            } else {
                defaults.removeObject(forKey: Self.carKey)
            }
        }
    }

    var isConfigured: Bool {
        !baseURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var endpoint: APIEndpoint {
        APIEndpoint(baseURL: baseURL, token: token)
    }

    init(defaults: UserDefaults = .standard, loadKeychain: Bool = true) {
        self.defaults = defaults
        baseURL = defaults.string(forKey: Self.urlKey) ?? ""
        token = loadKeychain ? KeychainToken.read() : ""
        if let stored = defaults.object(forKey: Self.carKey) as? Int, stored > 0 {
            selectedCarID = stored
        } else {
            selectedCarID = nil
        }
    }

    func save(baseURL: String, token: String) {
        self.baseURL = baseURL
        self.token = token.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func clear() {
        baseURL = ""
        token = ""
        selectedCarID = nil
        defaults.removeObject(forKey: Self.urlKey)
        defaults.removeObject(forKey: Self.carKey)
        KeychainToken.delete()
    }
}
