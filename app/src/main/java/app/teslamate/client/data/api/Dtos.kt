package app.teslamate.client.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CarsResponseDto(
    val data: CarsDataDto? = null,
    val error: String? = null,
)

@Serializable
data class CarsDataDto(
    val cars: List<CarDto>? = null,
)

@Serializable
data class CarDto(
    @SerialName("car_id") val carId: Int = 0,
    val name: String? = null,
    @SerialName("car_details") val details: CarDetailsDto = CarDetailsDto(),
    @SerialName("car_exterior") val exterior: CarExteriorDto = CarExteriorDto(),
    @SerialName("teslamate_stats") val stats: CarStatsDto = CarStatsDto(),
)

@Serializable
data class CarDetailsDto(
    val eid: Long = 0,
    val vid: Long = 0,
    val vin: String? = null,
    val model: String? = null,
    @SerialName("trim_badging") val trimBadging: String? = null,
    val efficiency: Double? = null,
)

@Serializable
data class CarExteriorDto(
    @SerialName("exterior_color") val exteriorColor: String? = null,
    @SerialName("spoiler_type") val spoilerType: String? = null,
    @SerialName("wheel_type") val wheelType: String? = null,
)

@Serializable
data class CarStatsDto(
    @SerialName("total_charges") val totalCharges: Int = 0,
    @SerialName("total_drives") val totalDrives: Int = 0,
    @SerialName("total_updates") val totalUpdates: Int = 0,
)

@Serializable
data class UnitsDto(
    @SerialName("unit_of_length") val length: String? = null,
    @SerialName("unit_of_temperature") val temperature: String? = null,
    @SerialName("unit_of_pressure") val pressure: String? = null,
)

@Serializable
data class NamedCarDto(
    @SerialName("car_id") val carId: Int = 0,
    @SerialName("car_name") val carName: String? = null,
)

@Serializable
data class StatusResponseDto(
    val data: StatusDataDto? = null,
    val error: String? = null,
)

@Serializable
data class StatusDataDto(
    val car: NamedCarDto = NamedCarDto(),
    val status: StatusDto = StatusDto(),
    val units: UnitsDto = UnitsDto(),
)

@Serializable
data class StatusDto(
    @SerialName("display_name") val displayName: String? = null,
    val state: String? = null,
    @SerialName("state_since") val stateSince: String? = null,
    val odometer: Double = 0.0,
    @SerialName("car_status") val carStatus: CarFlagsDto = CarFlagsDto(),
    @SerialName("car_details") val carDetails: StatusCarDetailsDto = StatusCarDetailsDto(),
    @SerialName("car_geodata") val geodata: GeoDto = GeoDto(),
    @SerialName("car_versions") val versions: VersionsDto = VersionsDto(),
    @SerialName("driving_details") val driving: DrivingDto = DrivingDto(),
    @SerialName("climate_details") val climate: ClimateDto = ClimateDto(),
    @SerialName("battery_details") val battery: StatusBatteryDto = StatusBatteryDto(),
    @SerialName("charging_details") val charging: ChargingDetailsDto = ChargingDetailsDto(),
    @SerialName("tpms_details") val tpms: TpmsDto = TpmsDto(),
)

@Serializable
data class CarFlagsDto(
    val healthy: Boolean = false,
    val locked: Boolean = false,
    @SerialName("sentry_mode") val sentryMode: Boolean = false,
    @SerialName("windows_open") val windowsOpen: Boolean = false,
    @SerialName("doors_open") val doorsOpen: Boolean = false,
    @SerialName("trunk_open") val trunkOpen: Boolean = false,
    @SerialName("frunk_open") val frunkOpen: Boolean = false,
    @SerialName("is_user_present") val userPresent: Boolean = false,
)

@Serializable
data class StatusCarDetailsDto(
    val model: String? = null,
    @SerialName("trim_badging") val trimBadging: String? = null,
)

@Serializable
data class GeoDto(
    val geofence: String? = null,
    val location: LatLngDto? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

@Serializable
data class LatLngDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

@Serializable
data class VersionsDto(
    val version: String? = null,
    @SerialName("update_available") val updateAvailable: Boolean = false,
    @SerialName("update_version") val updateVersion: String? = null,
)

@Serializable
data class DrivingDto(
    @SerialName("shift_state") val shiftState: String? = null,
    val power: Int = 0,
    val speed: Int = 0,
    val elevation: Int = 0,
)

@Serializable
data class ClimateDto(
    @SerialName("is_climate_on") val climateOn: Boolean = false,
    @SerialName("inside_temp") val insideTemp: Double = 0.0,
    @SerialName("outside_temp") val outsideTemp: Double = 0.0,
    @SerialName("is_preconditioning") val preconditioning: Boolean = false,
)

@Serializable
data class StatusBatteryDto(
    @SerialName("est_battery_range") val estRange: Double = 0.0,
    @SerialName("rated_battery_range") val ratedRange: Double = 0.0,
    @SerialName("ideal_battery_range") val idealRange: Double = 0.0,
    @SerialName("battery_level") val batteryLevel: Int = 0,
    @SerialName("usable_battery_level") val usableBatteryLevel: Int = 0,
)

@Serializable
data class ChargingDetailsDto(
    @SerialName("plugged_in") val pluggedIn: Boolean = false,
    @SerialName("charging_state") val chargingState: String? = null,
    @SerialName("charge_energy_added") val energyAdded: Double = 0.0,
    @SerialName("charge_limit_soc") val chargeLimitSoc: Int = 0,
    @SerialName("charge_port_door_open") val chargePortOpen: Boolean = false,
    @SerialName("charger_actual_current") val chargerCurrent: Double = 0.0,
    @SerialName("charger_phases") val chargerPhases: Int = 0,
    @SerialName("charger_power") val chargerPower: Double = 0.0,
    @SerialName("charger_voltage") val chargerVoltage: Int = 0,
    @SerialName("time_to_full_charge") val timeToFullCharge: Double = 0.0,
)

@Serializable
data class TpmsDto(
    @SerialName("tpms_pressure_fl") val fl: Double = 0.0,
    @SerialName("tpms_pressure_fr") val fr: Double = 0.0,
    @SerialName("tpms_pressure_rl") val rl: Double = 0.0,
    @SerialName("tpms_pressure_rr") val rr: Double = 0.0,
)

@Serializable
data class DrivesResponseDto(
    val data: DrivesDataDto? = null,
    val error: String? = null,
)

@Serializable
data class DrivesDataDto(
    val car: NamedCarDto = NamedCarDto(),
    val drives: List<DriveDto>? = null,
    val units: UnitsDto = UnitsDto(),
)

@Serializable
data class DriveDetailResponseDto(
    val data: DriveDetailDataDto? = null,
    val error: String? = null,
)

@Serializable
data class DriveDetailDataDto(
    val car: NamedCarDto = NamedCarDto(),
    val drive: DriveDto? = null,
    val units: UnitsDto = UnitsDto(),
)

@Serializable
data class DriveDto(
    @SerialName("drive_id") val driveId: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("start_address") val startAddress: String? = null,
    @SerialName("end_address") val endAddress: String? = null,
    @SerialName("odometer_details") val odometer: OdometerDto = OdometerDto(),
    @SerialName("duration_min") val durationMin: Int = 0,
    @SerialName("duration_str") val durationStr: String? = null,
    @SerialName("speed_max") val speedMax: Int = 0,
    @SerialName("speed_avg") val speedAvg: Double = 0.0,
    @SerialName("power_max") val powerMax: Int = 0,
    @SerialName("power_min") val powerMin: Int = 0,
    @SerialName("battery_details") val battery: DriveBatteryDto = DriveBatteryDto(),
    @SerialName("range_rated") val rangeRated: RangeDto = RangeDto(),
    @SerialName("outside_temp_avg") val outsideTemp: Double? = null,
    @SerialName("inside_temp_avg") val insideTemp: Double? = null,
    @SerialName("energy_consumed_net") val energyConsumedNet: Double? = null,
    @SerialName("consumption_net") val consumptionNet: Double? = null,
    @SerialName("drive_details") val driveDetails: List<JsonElement>? = null,
)

@Serializable
data class OdometerDto(
    @SerialName("odometer_start") val start: Double = 0.0,
    @SerialName("odometer_end") val end: Double = 0.0,
    @SerialName("odometer_distance") val distance: Double = 0.0,
)

@Serializable
data class DriveBatteryDto(
    @SerialName("start_battery_level") val start: Int = 0,
    @SerialName("end_battery_level") val end: Int = 0,
)

@Serializable
data class RangeDto(
    @SerialName("start_range") val start: Double = 0.0,
    @SerialName("end_range") val end: Double = 0.0,
    @SerialName("range_diff") val diff: Double = 0.0,
)

@Serializable
data class ChargesResponseDto(
    val data: ChargesDataDto? = null,
    val error: String? = null,
)

@Serializable
data class ChargesDataDto(
    val car: NamedCarDto = NamedCarDto(),
    val charges: List<ChargeDto>? = null,
    val units: UnitsDto = UnitsDto(),
)

@Serializable
data class ChargeDetailResponseDto(
    val data: ChargeDetailDataDto? = null,
    val error: String? = null,
)

@Serializable
data class ChargeDetailDataDto(
    val car: NamedCarDto = NamedCarDto(),
    val charge: ChargeDto? = null,
    val units: UnitsDto = UnitsDto(),
)

@Serializable
data class ChargeDto(
    @SerialName("charge_id") val chargeId: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val address: String? = null,
    @SerialName("charge_energy_added") val energyAdded: Double = 0.0,
    @SerialName("charge_energy_used") val energyUsed: Double = 0.0,
    val cost: Double = 0.0,
    @SerialName("duration_min") val durationMin: Int = 0,
    @SerialName("duration_str") val durationStr: String? = null,
    @SerialName("battery_details") val battery: ChargeBatteryDto = ChargeBatteryDto(),
    @SerialName("range_rated") val rangeRated: RangeDto = RangeDto(),
    @SerialName("outside_temp_avg") val outsideTemp: Double = 0.0,
    val odometer: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("charge_details") val chargeDetails: List<ChargePointDto>? = null,
)

@Serializable
data class ChargeBatteryDto(
    @SerialName("start_battery_level") val start: Int = 0,
    @SerialName("end_battery_level") val end: Int = 0,
)

@Serializable
data class ChargePointDto(
    @SerialName("detail_id") val detailId: Int = 0,
    @SerialName("charger_details") val charger: ChargeChargerDto = ChargeChargerDto(),
)

@Serializable
data class ChargeChargerDto(
    @SerialName("charger_actual_current") val current: Int = 0,
    @SerialName("charger_phases") val phases: Int = 0,
    @SerialName("charger_power") val power: Int = 0,
    @SerialName("charger_voltage") val voltage: Int = 0,
)

@Serializable
data class ErrorEnvelopeDto(
    val error: String? = null,
    val message: String? = null,
)
