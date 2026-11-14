import Foundation

/// Length and temperature already converted by TeslaMateApi from TeslaMate's settings.
/// Blank units fall back to kilometres and Celsius.
struct Units: Equatable, Sendable {
    var length: String
    var temperature: String
    var pressure: String?

    static let china = Units(length: "km", temperature: "C", pressure: "bar")

    init(length: String, temperature: String, pressure: String?) {
        let lengthCode = length.trimmingCharacters(in: .whitespacesAndNewlines)
        let temperatureCode = temperature.trimmingCharacters(in: .whitespacesAndNewlines)
        let pressureCode = pressure?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        self.length = lengthCode.lowercased() == "mi" ? "mi" : "km"
        self.temperature = temperatureCode.lowercased() == "f" ? "F" : "C"
        if pressureCode.isEmpty {
            self.pressure = nil
        } else {
            self.pressure = pressureCode.lowercased() == "psi" ? "psi" : "bar"
        }
    }

    var lengthLabel: String { length == "mi" ? "mi" : "km" }
    var temperatureLabel: String { temperature == "F" ? "°F" : "°C" }
    var speedLabel: String { length == "mi" ? "mph" : "km/h" }
    var consumptionLabel: String { length == "mi" ? "Wh/mi" : "Wh/km" }
    var pressureLabel: String { pressure == "psi" ? "psi" : "bar" }
}

struct Car: Identifiable, Equatable, Sendable {
    var id: Int
    var name: String
    var vin: String
    var model: String
    var trim: String
    var exteriorColor: String
    var wheelType: String
    var totalDrives: Int
    var totalCharges: Int
    var totalUpdates: Int
}

struct CarStatus: Equatable, Sendable {
    var carId: Int
    var carName: String
    var displayName: String
    var state: String
    var stateSince: String
    var odometer: Double
    var batteryLevel: Int
    var usableBatteryLevel: Int
    var estRange: Double
    var ratedRange: Double
    var idealRange: Double
    var insideTemp: Double
    var outsideTemp: Double
    var climateOn: Bool
    var preconditioning: Bool
    var pluggedIn: Bool
    var chargingState: String
    var chargeEnergyAdded: Double
    var chargeLimitSoc: Int
    var chargePortOpen: Bool
    var chargerPowerKw: Double
    var chargerVoltage: Int
    var chargerCurrent: Double
    var chargerPhases: Int
    var timeToFullHours: Double
    var latitude: Double
    var longitude: Double
    var geofence: String
    var locked: Bool
    var sentryMode: Bool
    var windowsOpen: Bool
    var doorsOpen: Bool
    var trunkOpen: Bool
    var frunkOpen: Bool
    var userPresent: Bool
    var softwareVersion: String
    var updateAvailable: Bool
    var updateVersion: String
    var model: String
    var trim: String
    var shiftState: String
    var speed: Int
    /// TeslaMateApi's comment says watts. The MQTT value is Tesla's integer power, which is kW.
    var powerKw: Int
    var elevation: Int
    var healthy: Bool
    var tpmsFl: Double
    var tpmsFr: Double
    var tpmsRl: Double
    var tpmsRr: Double
    var tpmsSoftFl: Bool
    var tpmsSoftFr: Bool
    var tpmsSoftRl: Bool
    var tpmsSoftRr: Bool
    var units: Units
}

struct DriveSummary: Identifiable, Equatable, Sendable {
    var id: Int
    var startDate: String
    var endDate: String
    var startAddress: String
    var endAddress: String
    var distance: Double
    var durationMin: Int
    var durationStr: String
    var speedMax: Int
    var speedAvg: Double
    var powerMax: Int
    var powerMin: Int
    var startBattery: Int
    var endBattery: Int
    var outsideTemp: Double?
    var insideTemp: Double?
    var energyConsumedNet: Double?
    var consumptionNet: Double?
    var ratedRangeStart: Double
    var ratedRangeEnd: Double
}

struct DrivePage: Equatable, Sendable {
    var carName: String
    var drives: [DriveSummary]
    var units: Units
}

struct DriveDetail: Equatable, Sendable {
    var drive: DriveSummary
    var trackPointCount: Int
    var units: Units
}

struct ChargeSummary: Identifiable, Equatable, Sendable {
    var id: Int
    var startDate: String
    var endDate: String
    var address: String
    var energyAdded: Double
    var energyUsed: Double
    var cost: Double
    var durationMin: Int
    var durationStr: String
    var startBattery: Int
    var endBattery: Int
    var ratedRangeStart: Double
    var ratedRangeEnd: Double
    var outsideTemp: Double
    var odometer: Double
    var latitude: Double
    var longitude: Double
}

struct ChargerPeak: Equatable, Sendable {
    var powerKw: Int
    var voltage: Int
    var phases: Int
    var current: Int
}

struct ChargePage: Equatable, Sendable {
    var carName: String
    var charges: [ChargeSummary]
    var units: Units
}

struct ChargeDetail: Equatable, Sendable {
    var charge: ChargeSummary
    var sampleCount: Int
    var chargerPeak: ChargerPeak?
    var units: Units
}
