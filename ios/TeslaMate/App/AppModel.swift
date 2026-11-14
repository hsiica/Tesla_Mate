import Foundation
import Observation

@MainActor
@Observable
final class AppModel {
    let settings: SettingsStore
    let client: TeslaMateClient

    var showMain: Bool
    var cars: [Car] = []
    var carsLoading = false
    var carsError: APIError?
    var didLoadCars = false
    var selectedCarID: Int?

    var selectedCar: Car? {
        if let selectedCarID, let match = cars.first(where: { $0.id == selectedCarID }) {
            return match
        }
        return cars.first
    }

    var endpoint: APIEndpoint { settings.endpoint }

    init(defaults: UserDefaults = .standard, loadKeychain: Bool = true, client: TeslaMateClient = TeslaMateClient()) {
        let settings = SettingsStore(defaults: defaults, loadKeychain: loadKeychain)
        self.settings = settings
        self.client = client
        selectedCarID = settings.selectedCarID
        showMain = settings.isConfigured
    }

    func refreshCars() async {
        guard settings.isConfigured else {
            carsError = .notConfigured
            didLoadCars = true
            return
        }
        if cars.isEmpty { carsLoading = true }
        defer { carsLoading = false }
        do {
            let list = try await client.cars(endpoint)
            cars = list
            carsError = nil
            didLoadCars = true
            reconcileSelection(with: list)
        } catch is CancellationError {
            return
        } catch let error as APIError {
            carsError = error
            didLoadCars = true
        } catch {
            carsError = .network
            didLoadCars = true
        }
    }

    func selectCar(_ id: Int) {
        selectedCarID = id
        settings.selectedCarID = id
    }

    func connect(url rawURL: String, token rawToken: String) async -> APIError? {
        guard let normalized = BaseURL.normalize(rawURL) else { return .invalidURL }
        let token = rawToken.trimmingCharacters(in: .whitespacesAndNewlines)
        settings.save(baseURL: normalized, token: token)
        do {
            let list = try await client.cars(settings.endpoint)
            cars = list
            carsError = nil
            didLoadCars = true
            reconcileSelection(with: list)
            showMain = true
            return nil
        } catch is CancellationError {
            return nil
        } catch let error as APIError {
            carsError = error
            didLoadCars = true
            return error
        } catch {
            carsError = .network
            didLoadCars = true
            return .network
        }
    }

    func clearSettings() {
        settings.clear()
        cars = []
        selectedCarID = nil
        carsError = nil
        didLoadCars = false
        showMain = false
    }

    private func reconcileSelection(with list: [Car]) {
        if let selectedCarID, list.contains(where: { $0.id == selectedCarID }) {
            settings.selectedCarID = selectedCarID
            return
        }
        if let saved = settings.selectedCarID, list.contains(where: { $0.id == saved }) {
            selectedCarID = saved
            return
        }
        selectedCarID = list.first?.id
        settings.selectedCarID = selectedCarID
    }
}
