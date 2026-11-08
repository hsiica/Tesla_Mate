package app.teslamate.client.domain

data class Units(
    val length: String,
    val temperature: String,
    val pressure: String? = null,
) {
    companion object {
        val Metric = Units(length = "km", temperature = "C", pressure = "bar")
    }
}

data class Car(
    val id: Int,
    val name: String,
    val vin: String,
    val model: String,
    val trim: String,
    val exteriorColor: String,
    val wheelType: String,
    val totalDrives: Int,
    val totalCharges: Int,
    val totalUpdates: Int,
)

data class CarStatus(
    val carId: Int,
    val carName: String,
    val displayName: String,
    val state: String,
    val stateSince: String,
    val odometer: Double,
    val batteryLevel: Int,
    val usableBatteryLevel: Int,
    val estRange: Double,
    val ratedRange: Double,
    val idealRange: Double,
    val insideTemp: Double,
    val outsideTemp: Double,
    val climateOn: Boolean,
    val preconditioning: Boolean,
    val pluggedIn: Boolean,
    val chargingState: String,
    val chargeEnergyAdded: Double,
    val chargeLimitSoc: Int,
    val chargePortOpen: Boolean,
    val chargerPowerKw: Double,
    val chargerVoltage: Int,
    val chargerCurrent: Double,
    val chargerPhases: Int,
    val timeToFullHours: Double,
    val latitude: Double,
    val longitude: Double,
    val geofence: String,
    val locked: Boolean,
    val sentryMode: Boolean,
    val windowsOpen: Boolean,
    val doorsOpen: Boolean,
    val trunkOpen: Boolean,
    val frunkOpen: Boolean,
    val userPresent: Boolean,
    val softwareVersion: String,
    val updateAvailable: Boolean,
    val updateVersion: String,
    val model: String,
    val trim: String,
    val shiftState: String,
    val speed: Int,
    // TeslaMateApi labels this "watts", but the MQTT payload is Tesla's integer power value, which is kW.
    val powerKw: Int,
    val elevation: Int,
    val healthy: Boolean,
    val tpmsFl: Double,
    val tpmsFr: Double,
    val tpmsRl: Double,
    val tpmsRr: Double,
    val units: Units,
)

data class DriveSummary(
    val id: Int,
    val startDate: String,
    val endDate: String,
    val startAddress: String,
    val endAddress: String,
    val distance: Double,
    val durationMin: Int,
    val durationStr: String,
    val speedMax: Int,
    val speedAvg: Double,
    val powerMax: Int,
    val powerMin: Int,
    val startBattery: Int,
    val endBattery: Int,
    val outsideTemp: Double?,
    val insideTemp: Double?,
    val energyConsumedNet: Double?,
    val consumptionNet: Double?,
    val ratedRangeStart: Double,
    val ratedRangeEnd: Double,
    val ratedRangeDiff: Double,
)

data class DrivePage(
    val carName: String,
    val drives: List<DriveSummary>,
    val units: Units,
)

data class DriveDetail(
    val drive: DriveSummary,
    val trackPointCount: Int,
    val units: Units,
)

data class ChargeSummary(
    val id: Int,
    val startDate: String,
    val endDate: String,
    val address: String,
    val energyAdded: Double,
    val energyUsed: Double,
    val cost: Double,
    val durationMin: Int,
    val durationStr: String,
    val startBattery: Int,
    val endBattery: Int,
    val ratedRangeStart: Double,
    val ratedRangeEnd: Double,
    val outsideTemp: Double,
    val odometer: Double,
    val latitude: Double,
    val longitude: Double,
)

data class ChargerPeak(
    val powerKw: Int,
    val voltage: Int,
    val phases: Int,
    val current: Int,
)

data class ChargePage(
    val carName: String,
    val charges: List<ChargeSummary>,
    val units: Units,
)

data class ChargeDetail(
    val charge: ChargeSummary,
    val sampleCount: Int,
    val chargerPeak: ChargerPeak?,
    val units: Units,
)
