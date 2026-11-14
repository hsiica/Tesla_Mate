import Foundation

/// Read-only client for [TeslaMateApi](https://github.com/tobiasehlert/teslamateapi).
///
/// Paths match `src/webserver.go`:
/// `GET /api/v1/cars`
/// `GET /api/v1/cars/:CarID/status`
/// `GET /api/v1/cars/:CarID/drives?page&show`
/// `GET /api/v1/cars/:CarID/drives/:DriveID`
/// `GET /api/v1/cars/:CarID/charges?page&show`
/// `GET /api/v1/cars/:CarID/charges/:ChargeID`
///
/// Failed queries are often HTTP 200 with `{"error":"..."}`.
/// `API_TOKEN` is not required for these reads. When one is saved, every request
/// sends `Authorization: Bearer`.
struct TeslaMateClient: Sendable {
    static let pageSize = 40

    var session: URLSession

    init(session: URLSession = TeslaMateClient.makeSession()) {
        self.session = session
    }

    func cars(_ endpoint: APIEndpoint) async throws -> [Car] {
        let json = try await fetch(endpoint, path: "api/v1/cars")
        let data = try requiredData(json)
        return data.array("cars").map { $0.toCar() }
    }

    func status(_ endpoint: APIEndpoint, carID: Int) async throws -> CarStatus {
        let json = try await fetch(endpoint, path: "api/v1/cars/\(carID)/status")
        let data = try requiredData(json)
        return try data.toStatus()
    }

    func drives(_ endpoint: APIEndpoint, carID: Int, page: Int, show: Int = pageSize) async throws -> DrivePage {
        let json = try await fetch(
            endpoint,
            path: "api/v1/cars/\(carID)/drives",
            query: pageQuery(page: page, show: show)
        )
        let data = try requiredData(json)
        return DrivePage(
            carName: data.object("car").string("car_name"),
            drives: data.array("drives").map { $0.toDrive() },
            units: data.object("units").toUnits()
        )
    }

    func drive(_ endpoint: APIEndpoint, carID: Int, driveID: Int) async throws -> DriveDetail {
        let json = try await fetch(endpoint, path: "api/v1/cars/\(carID)/drives/\(driveID)")
        let data = try requiredData(json)
        guard let drive = data.objectIfPresent("drive"), drive.int("drive_id") != 0 else {
            throw APIError.api("Empty response")
        }
        return DriveDetail(
            drive: drive.toDrive(),
            trackPointCount: drive.array("drive_details").count,
            units: data.object("units").toUnits()
        )
    }

    func charges(_ endpoint: APIEndpoint, carID: Int, page: Int, show: Int = pageSize) async throws -> ChargePage {
        let json = try await fetch(
            endpoint,
            path: "api/v1/cars/\(carID)/charges",
            query: pageQuery(page: page, show: show)
        )
        let data = try requiredData(json)
        return ChargePage(
            carName: data.object("car").string("car_name"),
            charges: data.array("charges").map { $0.toCharge() },
            units: data.object("units").toUnits()
        )
    }

    func charge(_ endpoint: APIEndpoint, carID: Int, chargeID: Int) async throws -> ChargeDetail {
        let json = try await fetch(endpoint, path: "api/v1/cars/\(carID)/charges/\(chargeID)")
        let data = try requiredData(json)
        guard let charge = data.objectIfPresent("charge"), charge.int("charge_id") != 0 else {
            throw APIError.api("Empty response")
        }
        let summary = charge.toCharge()
        let samples = charge.array("charge_details")
        return ChargeDetail(
            charge: summary,
            sampleCount: samples.count,
            chargerPeak: chargerPeak(from: samples),
            units: data.object("units").toUnits()
        )
    }

    private func pageQuery(page: Int, show: Int) -> [URLQueryItem] {
        [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "show", value: String(show)),
        ]
    }

    private func fetch(_ endpoint: APIEndpoint, path: String, query: [URLQueryItem] = []) async throws -> JSONValue {
        let base = endpoint.baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        if base.isEmpty { throw APIError.notConfigured }
        guard let url = APIEndpoint(baseURL: base, token: endpoint.token).url(path: path, query: query) else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let token = endpoint.token.trimmingCharacters(in: .whitespacesAndNewlines)
        if !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch is CancellationError {
            throw CancellationError()
        } catch let error as URLError where error.code == .cancelled {
            throw CancellationError()
        } catch {
            throw APIError.network
        }

        guard let http = response as? HTTPURLResponse else { throw APIError.network }
        let json: JSONValue
        do {
            json = try JSONDecoder().decode(JSONValue.self, from: data)
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            if (200 ... 299).contains(http.statusCode) {
                throw APIError.parse
            }
            throw APIError.http(http.statusCode, nil)
        }

        if !(200 ... 299).contains(http.statusCode) {
            throw APIError.http(http.statusCode, firstText(json.string("error"), json.string("message")))
        }
        if let message = firstText(json.string("error")) {
            throw APIError.api(message)
        }
        return json
    }

    private func requiredData(_ json: JSONValue) throws -> JSONValue {
        guard let data = json.objectIfPresent("data") else {
            throw APIError.api("Empty response")
        }
        return data
    }

    private func firstText(_ values: String...) -> String? {
        for value in values {
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
        }
        return nil
    }

    private static func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 60
        configuration.waitsForConnectivity = false
        return URLSession(configuration: configuration)
    }
}

private func chargerPeak(from samples: [JSONValue]) -> ChargerPeak? {
    let ranked = samples.max { lhs, rhs in
        lhs.object("charger_details").int("charger_power") < rhs.object("charger_details").int("charger_power")
    }
    guard let ranked else { return nil }
    let charger = ranked.object("charger_details")
    let power = charger.int("charger_power")
    let current = charger.int("charger_actual_current")
    guard power > 0 || current > 0 else { return nil }
    return ChargerPeak(
        powerKw: power,
        voltage: charger.int("charger_voltage"),
        phases: charger.int("charger_phases"),
        current: current
    )
}

private extension JSONValue {
    func toUnits() -> Units {
        Units(
            length: string("unit_of_length"),
            temperature: string("unit_of_temperature"),
            pressure: self["unit_of_pressure"] == nil ? nil : string("unit_of_pressure")
        )
    }

    func toCar() -> Car {
        let details = object("car_details")
        let exterior = object("car_exterior")
        let stats = object("teslamate_stats")
        return Car(
            id: int("car_id"),
            name: string("name"),
            vin: details.string("vin"),
            model: details.string("model"),
            trim: details.string("trim_badging"),
            exteriorColor: exterior.string("exterior_color"),
            wheelType: exterior.string("wheel_type"),
            totalDrives: stats.int("total_drives"),
            totalCharges: stats.int("total_charges"),
            totalUpdates: stats.int("total_updates")
        )
    }

    func toStatus() throws -> CarStatus {
        let car = object("car")
        let status = object("status")
        if car.int("car_id") == 0 && status.string("state").isEmpty && status.string("display_name").isEmpty {
            throw APIError.api("Empty response")
        }
        let flags = status.object("car_status")
        let battery = status.object("battery_details")
        let charging = status.object("charging_details")
        let climate = status.object("climate_details")
        let driving = status.object("driving_details")
        let versions = status.object("car_versions")
        let details = status.object("car_details")
        let tpms = status.object("tpms_details")
        let geo = status.object("car_geodata")
        let location = geo.object("location")
        let locationLatitude = location.double("latitude")
        let locationLongitude = location.double("longitude")
        let latitude: Double
        let longitude: Double
        if locationLatitude != 0 || locationLongitude != 0 {
            latitude = locationLatitude
            longitude = locationLongitude
        } else {
            latitude = geo.double("latitude")
            longitude = geo.double("longitude")
        }
        return CarStatus(
            carId: car.int("car_id"),
            carName: car.string("car_name"),
            displayName: status.string("display_name"),
            state: status.string("state"),
            stateSince: status.string("state_since"),
            odometer: status.double("odometer"),
            batteryLevel: battery.int("battery_level"),
            usableBatteryLevel: battery.int("usable_battery_level"),
            estRange: battery.double("est_battery_range"),
            ratedRange: battery.double("rated_battery_range"),
            idealRange: battery.double("ideal_battery_range"),
            insideTemp: climate.double("inside_temp"),
            outsideTemp: climate.double("outside_temp"),
            climateOn: climate.bool("is_climate_on"),
            preconditioning: climate.bool("is_preconditioning"),
            pluggedIn: charging.bool("plugged_in"),
            chargingState: charging.string("charging_state"),
            chargeEnergyAdded: charging.double("charge_energy_added"),
            chargeLimitSoc: charging.int("charge_limit_soc"),
            chargePortOpen: charging.bool("charge_port_door_open"),
            chargerPowerKw: charging.double("charger_power"),
            chargerVoltage: charging.int("charger_voltage"),
            chargerCurrent: charging.double("charger_actual_current"),
            chargerPhases: charging.int("charger_phases"),
            timeToFullHours: charging.double("time_to_full_charge"),
            latitude: latitude,
            longitude: longitude,
            geofence: geo.string("geofence"),
            locked: flags.bool("locked"),
            sentryMode: flags.bool("sentry_mode"),
            windowsOpen: flags.bool("windows_open"),
            doorsOpen: flags.bool("doors_open"),
            trunkOpen: flags.bool("trunk_open"),
            frunkOpen: flags.bool("frunk_open"),
            userPresent: flags.bool("is_user_present"),
            softwareVersion: versions.string("version"),
            updateAvailable: versions.bool("update_available"),
            updateVersion: versions.string("update_version"),
            model: details.string("model"),
            trim: details.string("trim_badging"),
            shiftState: driving.string("shift_state"),
            speed: driving.int("speed"),
            powerKw: driving.int("power"),
            elevation: driving.int("elevation"),
            healthy: flags.bool("healthy"),
            tpmsFl: tpms.double("tpms_pressure_fl"),
            tpmsFr: tpms.double("tpms_pressure_fr"),
            tpmsRl: tpms.double("tpms_pressure_rl"),
            tpmsRr: tpms.double("tpms_pressure_rr"),
            tpmsSoftFl: tpms.bool("tpms_soft_warning_fl"),
            tpmsSoftFr: tpms.bool("tpms_soft_warning_fr"),
            tpmsSoftRl: tpms.bool("tpms_soft_warning_rl"),
            tpmsSoftRr: tpms.bool("tpms_soft_warning_rr"),
            units: object("units").toUnits()
        )
    }

    func toDrive() -> DriveSummary {
        let odometer = object("odometer_details")
        let battery = object("battery_details")
        let rated = object("range_rated")
        return DriveSummary(
            id: int("drive_id"),
            startDate: string("start_date"),
            endDate: string("end_date"),
            startAddress: string("start_address"),
            endAddress: string("end_address"),
            distance: odometer.double("odometer_distance"),
            durationMin: int("duration_min"),
            durationStr: string("duration_str"),
            speedMax: int("speed_max"),
            speedAvg: double("speed_avg"),
            powerMax: int("power_max"),
            powerMin: int("power_min"),
            startBattery: battery.int("start_battery_level"),
            endBattery: battery.int("end_battery_level"),
            outsideTemp: optionalDouble("outside_temp_avg"),
            insideTemp: optionalDouble("inside_temp_avg"),
            energyConsumedNet: optionalDouble("energy_consumed_net"),
            consumptionNet: optionalDouble("consumption_net"),
            ratedRangeStart: rated.double("start_range"),
            ratedRangeEnd: rated.double("end_range")
        )
    }

    func toCharge() -> ChargeSummary {
        let battery = object("battery_details")
        let rated = object("range_rated")
        return ChargeSummary(
            id: int("charge_id"),
            startDate: string("start_date"),
            endDate: string("end_date"),
            address: string("address"),
            energyAdded: double("charge_energy_added"),
            energyUsed: double("charge_energy_used"),
            cost: double("cost"),
            durationMin: int("duration_min"),
            durationStr: string("duration_str"),
            startBattery: battery.int("start_battery_level"),
            endBattery: battery.int("end_battery_level"),
            ratedRangeStart: rated.double("start_range"),
            ratedRangeEnd: rated.double("end_range"),
            outsideTemp: double("outside_temp_avg"),
            odometer: double("odometer"),
            latitude: double("latitude"),
            longitude: double("longitude")
        )
    }
}
