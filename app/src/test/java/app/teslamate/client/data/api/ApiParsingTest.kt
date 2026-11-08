package app.teslamate.client.data.api

import app.teslamate.client.domain.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiParsingTest {
    @Test
    fun carsList_matchesTeslaMateApiShape_includingNullStrings() {
        val cars = ApiJson.json.decodeFromString<CarsResponseDto>(CARS_JSON).toCars()
        assertEquals(1, cars.size)
        val car = cars.single()
        assertEquals(1, car.id)
        assertEquals("MJ3", car.name)
        assertEquals("3", car.model)
        assertEquals("", car.trim)
        assertEquals("PearlWhite", car.exteriorColor)
        assertEquals("PinwheelRefresh18", car.wheelType)
        assertEquals(12, car.totalCharges)
        assertEquals(34, car.totalDrives)
        assertEquals(2, car.totalUpdates)
        assertTrue(car.vin.startsWith("5YJ3"))
    }

    @Test
    fun carsError_andNullList() {
        val error = ApiJson.json.decodeFromString<CarsResponseDto>("""{"error":"Unable to load cars."}""")
        val thrown = runCatching { error.toCars() }.exceptionOrNull()
        assertTrue(thrown is AppException.Api)
        assertEquals("Unable to load cars.", thrown?.message)

        val empty = ApiJson.json.decodeFromString<CarsResponseDto>(
            """{"data":{"cars":null}}""",
        ).toCars()
        assertTrue(empty.isEmpty())
    }

    @Test
    fun status_parsesCurrentSchema_andLegacyPayloadMissingNewerFields() {
        val modern = ApiJson.json.decodeFromString<StatusResponseDto>(STATUS_JSON).toStatus()
        assertEquals(1, modern.carId)
        assertEquals("MJ3", modern.carName)
        assertEquals("online", modern.state)
        assertEquals(88, modern.batteryLevel)
        assertEquals(85, modern.usableBatteryLevel)
        assertEquals(372.5, modern.estRange, 0.001)
        assertEquals(401.63, modern.ratedRange, 0.001)
        assertEquals(31.2, modern.latitude, 0.001)
        assertEquals(121.5, modern.longitude, 0.001)
        assertEquals("家", modern.geofence)
        assertEquals("2024.44.3", modern.softwareVersion)
        assertTrue(modern.locked)
        assertEquals(11.5, modern.chargerPowerKw, 0.001)
        assertEquals("Charging", modern.chargingState)
        assertEquals(2.4, modern.tpmsFl, 0.001)
        assertEquals(Units("km", "C", "bar"), modern.units)
        assertEquals(-9, modern.powerKw)
        assertEquals(12, modern.speed)

        val legacy = ApiJson.json.decodeFromString<StatusResponseDto>(LEGACY_STATUS_JSON).toStatus()
        assertEquals(1, legacy.carId)
        assertEquals("", legacy.state)
        assertEquals(0, legacy.batteryLevel)
        assertEquals("", legacy.chargingState)
        assertEquals(0.0, legacy.tpmsFl, 0.001)
        assertEquals(Units.Metric.length, legacy.units.length)
    }

    @Test
    fun status_nullBatteryLevel_isCoercedToDefault() {
        val parsed = ApiJson.json.decodeFromString<StatusResponseDto>(
            """
            {"data":{"car":{"car_id":1,"car_name":"MJ3"},"status":{"state":"online","battery_details":{"battery_level":null,"usable_battery_level":70,"rated_battery_range":300}},"units":{"unit_of_length":"km","unit_of_temperature":"C"}}}
            """.trimIndent(),
        ).toStatus()
        assertEquals(0, parsed.batteryLevel)
        assertEquals(70, parsed.usableBatteryLevel)
        assertEquals(300.0, parsed.ratedRange, 0.001)
    }

    @Test
    fun drives_nullableEnergy_andNullList() {
        val page = ApiJson.json.decodeFromString<DrivesResponseDto>(DRIVES_JSON).toDrivePage()
        assertEquals("MJ3", page.carName)
        assertEquals("km", page.units.length)
        assertEquals(1, page.drives.size)
        val drive = page.drives.single()
        assertEquals(7, drive.id)
        assertEquals(18.4, drive.distance, 0.001)
        assertEquals(42, drive.durationMin)
        assertEquals("00:42", drive.durationStr)
        assertEquals(80, drive.startBattery)
        assertEquals(71, drive.endBattery)
        assertEquals(3.2, drive.energyConsumedNet!!, 0.001)
        assertNull(drive.insideTemp)
        assertEquals(22.0, drive.outsideTemp!!, 0.001)
        assertEquals(128, drive.speedMax)
        assertNull(drive.consumptionNet)

        val none = ApiJson.json.decodeFromString<DrivesResponseDto>(
            """{"data":{"car":{"car_id":1,"car_name":null},"drives":null,"units":{"unit_of_length":"mi","unit_of_temperature":"F"}}}""",
        ).toDrivePage()
        assertTrue(none.drives.isEmpty())
        assertEquals("", none.carName)
        assertEquals(Units("mi", "F", null), none.units)
    }

    @Test
    fun driveDetail_countsTrackPoints_andIgnoresUnknownFields() {
        val detail = ApiJson.json.decodeFromString<DriveDetailResponseDto>(DRIVE_DETAIL_JSON).toDriveDetail()
        assertEquals(7, detail.drive.id)
        assertEquals(2, detail.trackPointCount)
        assertEquals(12.5, detail.drive.distance, 0.001)
    }

    @Test
    fun charges_andDetailPeakPower() {
        val page = ApiJson.json.decodeFromString<ChargesResponseDto>(CHARGES_JSON).toChargePage()
        val charge = page.charges.single()
        assertEquals(3, charge.id)
        assertEquals(18.5, charge.energyAdded, 0.001)
        assertEquals(4.5, charge.cost, 0.001)
        assertEquals(25, charge.startBattery)
        assertEquals(80, charge.endBattery)
        assertEquals("家", charge.address)
        assertEquals(150, charge.durationMin)

        val detail = ApiJson.json.decodeFromString<ChargeDetailResponseDto>(CHARGE_DETAIL_JSON).toChargeDetail()
        assertEquals(3, detail.charge.id)
        assertEquals(2, detail.sampleCount)
        assertEquals(11, detail.chargerPeak?.powerKw)
        assertEquals(230, detail.chargerPeak?.voltage)
        assertEquals(3, detail.chargerPeak?.phases)
        assertEquals(16, detail.chargerPeak?.current)
    }

    @Test
    fun drivesErrorEnvelope() {
        val thrown = runCatching {
            ApiJson.json.decodeFromString<DrivesResponseDto>("""{"error":"Unable to load drives."}""").toDrivePage()
        }.exceptionOrNull()
        assertTrue(thrown is AppException.Api)
        assertEquals("Unable to load drives.", thrown?.message)
    }
}

private const val CARS_JSON = """
{
  "data": {
    "cars": [
      {
        "car_id": 1,
        "name": "MJ3",
        "car_details": {
          "eid": 194379021111,
          "vid": 182492171111,
          "vin": "5YJ3E7EA2MF000000",
          "model": "3",
          "trim_badging": null,
          "efficiency": 0.152
        },
        "car_exterior": {
          "exterior_color": "PearlWhite",
          "spoiler_type": "None",
          "wheel_type": "PinwheelRefresh18"
        },
        "car_settings": {
          "suspend_min": 12,
          "suspend_after_idle_min": 15,
          "req_not_unlocked": false,
          "free_supercharging": false,
          "use_streaming_api": true
        },
        "teslamate_details": {
          "inserted_at": "2021-03-13T12:22:57+01:00",
          "updated_at": "2021-03-15T09:54:24+01:00"
        },
        "teslamate_stats": {
          "total_charges": 12,
          "total_drives": 34,
          "total_updates": 2
        },
        "future_field": true
      }
    ]
  }
}
"""

private const val STATUS_JSON = """
{
  "data": {
    "car": { "car_id": 1, "car_name": "MJ3" },
    "status": {
      "display_name": "MJ3",
      "state": "online",
      "state_since": "2024-08-18T08:15:00+08:00",
      "odometer": 16531.2,
      "car_status": {
        "healthy": true,
        "locked": true,
        "sentry_mode": false,
        "windows_open": false,
        "doors_open": false,
        "driver_front_door_open": false,
        "driver_rear_door_open": false,
        "passenger_front_door_open": false,
        "passenger_rear_door_open": false,
        "trunk_open": false,
        "frunk_open": false,
        "is_user_present": false,
        "center_display_state": 0
      },
      "car_details": { "model": "3", "trim_badging": "74D" },
      "car_exterior": { "exterior_color": "PearlWhite", "spoiler_type": "None", "wheel_type": "Pinwheel18" },
      "car_geodata": {
        "geofence": "家",
        "location": { "latitude": 31.2, "longitude": 121.5 },
        "latitude": 31.2,
        "longitude": 121.5
      },
      "car_versions": { "version": "2024.44.3", "update_available": false, "update_version": "" },
      "driving_details": {
        "active_route": {
          "destination": "",
          "energy_at_arrival": 0,
          "distance_to_arrival": 0,
          "minutes_to_arrival": 0,
          "traffic_minutes_delay": 0,
          "location": { "latitude": 0, "longitude": 0 }
        },
        "shift_state": "P",
        "power": -9,
        "speed": 12,
        "heading": 340,
        "elevation": 12
      },
      "climate_details": {
        "is_climate_on": false,
        "inside_temp": 23.4,
        "outside_temp": 31.0,
        "is_preconditioning": false,
        "climate_keeper_mode": "off"
      },
      "battery_details": {
        "est_battery_range": 372.5,
        "rated_battery_range": 401.63,
        "ideal_battery_range": 335.79,
        "battery_level": 88,
        "usable_battery_level": 85
      },
      "charging_details": {
        "plugged_in": true,
        "charging_state": "Charging",
        "charge_energy_added": 5.06,
        "charge_limit_soc": 90,
        "charge_port_door_open": true,
        "charger_actual_current": 16.0,
        "charger_phases": 3,
        "charger_power": 11.5,
        "charger_voltage": 230,
        "charge_current_request": 16,
        "charge_current_request_max": 32,
        "scheduled_charging_start_time": "2024-08-18T23:00:00+08:00",
        "time_to_full_charge": 1.83
      },
      "tpms_details": {
        "tpms_pressure_fl": 2.4,
        "tpms_pressure_fr": 2.4,
        "tpms_pressure_rl": 2.5,
        "tpms_pressure_rr": 2.5,
        "tpms_soft_warning_fl": false,
        "tpms_soft_warning_fr": false,
        "tpms_soft_warning_rl": false,
        "tpms_soft_warning_rr": false
      }
    },
    "units": { "unit_of_length": "km", "unit_of_pressure": "bar", "unit_of_temperature": "C" }
  }
}
"""

private const val LEGACY_STATUS_JSON = """
{
  "data": {
    "car": { "car_id": 1, "car_name": "MJ3" },
    "status": {
      "display_name": "",
      "state": "",
      "state_since": "0001-01-01T00:53:28+00:53",
      "odometer": 0,
      "car_status": {
        "healthy": false,
        "locked": false,
        "sentry_mode": false,
        "windows_open": false,
        "doors_open": false,
        "trunk_open": false,
        "frunk_open": false,
        "is_user_present": false
      },
      "car_details": { "model": "", "trim_badging": "" },
      "car_exterior": { "exterior_color": "", "spoiler_type": "", "wheel_type": "" },
      "car_geodata": { "geofence": "", "latitude": 0, "longitude": 0 },
      "car_versions": { "version": "", "update_available": false, "update_version": "" },
      "driving_details": { "shift_state": "", "speed": 0, "heading": 0, "elevation": 0 },
      "climate_details": { "is_climate_on": false, "inside_temp": 0, "outside_temp": 0, "is_preconditioning": false },
      "battery_details": {
        "est_battery_range": 0,
        "rated_battery_range": 0,
        "ideal_battery_range": 0,
        "battery_level": 0,
        "usable_battery_level": 0
      },
      "charging_details": {
        "plugged_in": false,
        "charge_energy_added": 0,
        "charge_limit_soc": 0,
        "charge_port_door_open": false,
        "charger_actual_current": 0,
        "charger_phases": 0,
        "charger_power": 0,
        "charger_voltage": 0,
        "scheduled_charging_start_time": "0001-01-01T00:53:28+00:53",
        "time_to_full_charge": 0
      }
    },
    "units": { "unit_of_length": "km", "unit_of_temperature": "C" }
  }
}
"""

private const val DRIVES_JSON = """
{
  "data": {
    "car": { "car_id": 1, "car_name": "MJ3" },
    "drives": [
      {
        "drive_id": 7,
        "start_date": "2024-08-18T08:15:00+08:00",
        "end_date": "2024-08-18T08:57:00+08:00",
        "start_address": "家",
        "end_address": "公司",
        "odometer_details": { "odometer_start": 16500.0, "odometer_end": 16518.4, "odometer_distance": 18.4 },
        "duration_min": 42,
        "duration_str": "00:42",
        "speed_max": 128,
        "speed_avg": 26.3,
        "power_max": 180,
        "power_min": -60,
        "battery_details": {
          "start_usable_battery_level": 80,
          "start_battery_level": 80,
          "end_usable_battery_level": 71,
          "end_battery_level": 71,
          "reduced_range": false,
          "is_sufficiently_precise": true
        },
        "range_ideal": { "start_range": 400, "end_range": 360, "range_diff": 40 },
        "range_rated": { "start_range": 420.0, "end_range": 390.5, "range_diff": 29.5 },
        "outside_temp_avg": 22.0,
        "inside_temp_avg": null,
        "energy_consumed_net": 3.2,
        "consumption_net": null
      }
    ],
    "units": { "unit_of_length": "km", "unit_of_temperature": "C" }
  }
}
"""

private const val DRIVE_DETAIL_JSON = """
{
  "data": {
    "car": { "car_id": 1, "car_name": "MJ3" },
    "drive": {
      "drive_id": 7,
      "start_date": "2024-08-18T08:15:00+08:00",
      "end_date": "2024-08-18T08:40:00+08:00",
      "start_address": "家",
      "end_address": "公司",
      "odometer_details": { "odometer_distance": 12.5 },
      "duration_min": 25,
      "duration_str": "00:25",
      "drive_details": [
        { "detail_id": 1, "latitude": 31.2, "longitude": 121.5, "unknown": 1 },
        { "detail_id": 2, "latitude": 31.3, "longitude": 121.6 }
      ]
    },
    "units": { "unit_of_length": "km", "unit_of_temperature": "C" }
  }
}
"""

private const val CHARGES_JSON = """
{
  "data": {
    "car": { "car_id": 1, "car_name": "MJ3" },
    "charges": [
      {
        "charge_id": 3,
        "start_date": "2024-08-18T10:15:00+08:00",
        "end_date": "2024-08-18T12:45:00+08:00",
        "address": "家",
        "charge_energy_added": 18.5,
        "charge_energy_used": 19.1,
        "cost": 4.5,
        "duration_min": 150,
        "duration_str": "02:30",
        "battery_details": { "start_battery_level": 25, "end_battery_level": 80 },
        "range_ideal": { "start_range": 120.5, "end_range": 412.5 },
        "range_rated": { "start_range": 131.5, "end_range": 450.0 },
        "outside_temp_avg": 22.5,
        "odometer": 15432.5,
        "latitude": 31.2,
        "longitude": 121.5
      }
    ],
    "units": { "unit_of_length": "km", "unit_of_temperature": "C" }
  }
}
"""

private const val CHARGE_DETAIL_JSON = """
{
  "data": {
    "car": { "car_id": 1, "car_name": "MJ3" },
    "charge": {
      "charge_id": 3,
      "start_date": "2024-08-18T10:15:00+08:00",
      "end_date": "2024-08-18T12:45:00+08:00",
      "address": "家",
      "charge_energy_added": 18.5,
      "charge_energy_used": 19.1,
      "cost": 4.5,
      "duration_min": 150,
      "duration_str": "02:30",
      "battery_details": { "start_battery_level": 25, "end_battery_level": 80 },
      "range_rated": { "start_range": 131.5, "end_range": 450 },
      "outside_temp_avg": 22.5,
      "odometer": 15432.5,
      "latitude": 31.2,
      "longitude": 121.5,
      "charge_details": [
        {
          "detail_id": 1,
          "date": "2024-08-18T10:16:00+08:00",
          "battery_level": 26,
          "charger_details": { "charger_actual_current": 10, "charger_phases": 1, "charger_power": 2, "charger_voltage": 220 }
        },
        {
          "detail_id": 2,
          "battery_level": 40,
          "charger_details": { "charger_actual_current": 16, "charger_phases": 3, "charger_power": 11, "charger_voltage": 230 }
        }
      ]
    },
    "units": { "unit_of_length": "km", "unit_of_temperature": "C" }
  }
}
"""
