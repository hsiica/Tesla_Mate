package app.teslamate.client.data.api

import app.teslamate.client.domain.Car
import app.teslamate.client.domain.CarStatus
import app.teslamate.client.domain.ChargeDetail
import app.teslamate.client.domain.ChargePage
import app.teslamate.client.domain.ChargeSummary
import app.teslamate.client.domain.ChargerPeak
import app.teslamate.client.domain.DriveDetail
import app.teslamate.client.domain.DrivePage
import app.teslamate.client.domain.DriveSummary
import app.teslamate.client.domain.Units

fun CarsResponseDto.toCars(): List<Car> {
    if (!error.isNullOrBlank()) throw AppException.Api(error)
    val payload = data ?: throw AppException.Api("Empty response")
    return payload.cars.orEmpty().map { it.toDomain() }
}

fun CarDto.toDomain(): Car = Car(
    id = carId,
    name = name.orEmpty(),
    vin = details.vin.orEmpty(),
    model = details.model.orEmpty(),
    trim = details.trimBadging.orEmpty(),
    exteriorColor = exterior.exteriorColor.orEmpty(),
    wheelType = exterior.wheelType.orEmpty(),
    totalDrives = stats.totalDrives,
    totalCharges = stats.totalCharges,
    totalUpdates = stats.totalUpdates,
)

fun StatusResponseDto.toStatus(): CarStatus {
    if (!error.isNullOrBlank()) throw AppException.Api(error)
    val payload = data ?: throw AppException.Api("Empty response")
    if (payload.car.carId == 0 && payload.status.state.isNullOrBlank() && payload.status.displayName.isNullOrBlank()) {
        throw AppException.Api("Empty response")
    }
    return payload.toDomain()
}

private fun StatusDataDto.toDomain(): CarStatus {
    val geoLat = when {
        geodataHasLocation(status.geodata) -> status.geodata.location?.latitude ?: status.geodata.latitude
        else -> status.geodata.latitude
    }
    val geoLon = when {
        geodataHasLocation(status.geodata) -> status.geodata.location?.longitude ?: status.geodata.longitude
        else -> status.geodata.longitude
    }
    return CarStatus(
        carId = car.carId,
        carName = car.carName.orEmpty(),
        displayName = status.displayName.orEmpty(),
        state = status.state.orEmpty(),
        stateSince = status.stateSince.orEmpty(),
        odometer = status.odometer,
        batteryLevel = status.battery.batteryLevel,
        usableBatteryLevel = status.battery.usableBatteryLevel,
        estRange = status.battery.estRange,
        ratedRange = status.battery.ratedRange,
        idealRange = status.battery.idealRange,
        insideTemp = status.climate.insideTemp,
        outsideTemp = status.climate.outsideTemp,
        climateOn = status.climate.climateOn,
        preconditioning = status.climate.preconditioning,
        pluggedIn = status.charging.pluggedIn,
        chargingState = status.charging.chargingState.orEmpty(),
        chargeEnergyAdded = status.charging.energyAdded,
        chargeLimitSoc = status.charging.chargeLimitSoc,
        chargePortOpen = status.charging.chargePortOpen,
        chargerPowerKw = status.charging.chargerPower,
        chargerVoltage = status.charging.chargerVoltage,
        chargerCurrent = status.charging.chargerCurrent,
        chargerPhases = status.charging.chargerPhases,
        timeToFullHours = status.charging.timeToFullCharge,
        latitude = geoLat,
        longitude = geoLon,
        geofence = status.geodata.geofence.orEmpty(),
        locked = status.carStatus.locked,
        sentryMode = status.carStatus.sentryMode,
        windowsOpen = status.carStatus.windowsOpen,
        doorsOpen = status.carStatus.doorsOpen,
        trunkOpen = status.carStatus.trunkOpen,
        frunkOpen = status.carStatus.frunkOpen,
        userPresent = status.carStatus.userPresent,
        softwareVersion = status.versions.version.orEmpty(),
        updateAvailable = status.versions.updateAvailable,
        updateVersion = status.versions.updateVersion.orEmpty(),
        model = status.carDetails.model.orEmpty(),
        trim = status.carDetails.trimBadging.orEmpty(),
        shiftState = status.driving.shiftState.orEmpty(),
        speed = status.driving.speed,
        powerKw = status.driving.power,
        elevation = status.driving.elevation,
        healthy = status.carStatus.healthy,
        tpmsFl = status.tpms.fl,
        tpmsFr = status.tpms.fr,
        tpmsRl = status.tpms.rl,
        tpmsRr = status.tpms.rr,
        units = units.toDomain(),
    )
}

private fun geodataHasLocation(geo: GeoDto): Boolean {
    val location = geo.location ?: return false
    return location.latitude != 0.0 || location.longitude != 0.0
}

fun DrivesResponseDto.toDrivePage(): DrivePage {
    if (!error.isNullOrBlank()) throw AppException.Api(error)
    val payload = data ?: throw AppException.Api("Empty response")
    return DrivePage(
        carName = payload.car.carName.orEmpty(),
        drives = payload.drives.orEmpty().map { it.toDomain() },
        units = payload.units.toDomain(),
    )
}

fun DriveDetailResponseDto.toDriveDetail(): DriveDetail {
    if (!error.isNullOrBlank()) throw AppException.Api(error)
    val payload = data ?: throw AppException.Api("Empty response")
    val drive = payload.drive ?: throw AppException.Api("Empty response")
    if (drive.driveId == 0) throw AppException.Api("Empty response")
    return DriveDetail(
        drive = drive.toDomain(),
        trackPointCount = drive.driveDetails?.size ?: 0,
        units = payload.units.toDomain(),
    )
}

fun ChargesResponseDto.toChargePage(): ChargePage {
    if (!error.isNullOrBlank()) throw AppException.Api(error)
    val payload = data ?: throw AppException.Api("Empty response")
    return ChargePage(
        carName = payload.car.carName.orEmpty(),
        charges = payload.charges.orEmpty().map { it.toDomain() },
        units = payload.units.toDomain(),
    )
}

fun ChargeDetailResponseDto.toChargeDetail(): ChargeDetail {
    if (!error.isNullOrBlank()) throw AppException.Api(error)
    val payload = data ?: throw AppException.Api("Empty response")
    val charge = payload.charge ?: throw AppException.Api("Empty response")
    if (charge.chargeId == 0) throw AppException.Api("Empty response")
    val peak = charge.chargeDetails.orEmpty()
        .maxByOrNull { it.charger.power }
        ?.takeIf { it.charger.power > 0 || it.charger.current > 0 }
        ?.let {
            ChargerPeak(
                powerKw = it.charger.power,
                voltage = it.charger.voltage,
                phases = it.charger.phases,
                current = it.charger.current,
            )
        }
    return ChargeDetail(
        charge = charge.toDomain(),
        sampleCount = charge.chargeDetails?.size ?: 0,
        chargerPeak = peak,
        units = payload.units.toDomain(),
    )
}

private fun DriveDto.toDomain(): DriveSummary = DriveSummary(
    id = driveId,
    startDate = startDate.orEmpty(),
    endDate = endDate.orEmpty(),
    startAddress = startAddress.orEmpty(),
    endAddress = endAddress.orEmpty(),
    distance = odometer.distance,
    durationMin = durationMin,
    durationStr = durationStr.orEmpty(),
    speedMax = speedMax,
    speedAvg = speedAvg,
    powerMax = powerMax,
    powerMin = powerMin,
    startBattery = battery.start,
    endBattery = battery.end,
    outsideTemp = outsideTemp,
    insideTemp = insideTemp,
    energyConsumedNet = energyConsumedNet,
    consumptionNet = consumptionNet,
    ratedRangeStart = rangeRated.start,
    ratedRangeEnd = rangeRated.end,
    ratedRangeDiff = rangeRated.diff,
)

private fun ChargeDto.toDomain(): ChargeSummary = ChargeSummary(
    id = chargeId,
    startDate = startDate.orEmpty(),
    endDate = endDate.orEmpty(),
    address = address.orEmpty(),
    energyAdded = energyAdded,
    energyUsed = energyUsed,
    cost = cost,
    durationMin = durationMin,
    durationStr = durationStr.orEmpty(),
    startBattery = battery.start,
    endBattery = battery.end,
    ratedRangeStart = rangeRated.start,
    ratedRangeEnd = rangeRated.end,
    outsideTemp = outsideTemp,
    odometer = odometer,
    latitude = latitude,
    longitude = longitude,
)

fun UnitsDto.toDomain(): Units = Units(
    length = if (length.equals("mi", ignoreCase = true)) "mi" else "km",
    temperature = if (temperature.equals("F", ignoreCase = true)) "F" else "C",
    pressure = pressure?.takeIf { it.isNotBlank() },
)
