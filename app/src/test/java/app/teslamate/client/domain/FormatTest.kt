package app.teslamate.client.domain

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    @Test
    fun modelNames() {
        assertEquals("Model 3", formatTeslaModel("3"))
        assertEquals("Model Y", formatTeslaModel("y"))
        assertEquals("Cybertruck", formatTeslaModel("cybertruck"))
    }

    @Test
    fun datesUseZoneAndHideZeroYear() {
        assertEquals(
            "2024-08-18 16:15",
            formatApiDate("2024-08-18T10:15:00+02:00", ZoneId.of("Asia/Shanghai")),
        )
        assertEquals("—", formatApiDate("0001-01-01T00:53:28+00:53"))
        assertEquals("—", formatApiDate(""))
    }

    @Test
    fun hoursAndUnits() {
        assertEquals(HourMinute(1, 30), splitHours(1.5))
        assertNull(splitHours(0.0))
        assertEquals("km", lengthUnitLabel("km"))
        assertEquals("mph", speedUnitLabel("mi"))
        assertEquals("°C", tempUnitLabel("C"))
        assertEquals("Wh/km", consumptionUnitLabel("km"))
        assertEquals("01:05", formatDuration("", 65))
        assertEquals("02:30", formatDuration("02:30", 150))
    }
}
