package app.teslamate.client.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlNormalizerTest {
    @Test
    fun stripsTrailingSlashAndApiSuffix() {
        assertEquals("http://192.168.1.10:8080", normalizeBaseUrl("http://192.168.1.10:8080/"))
        assertEquals("http://192.168.1.10:8080", normalizeBaseUrl("http://192.168.1.10:8080/api/v1"))
        assertEquals("http://192.168.1.10:8080", normalizeBaseUrl("http://192.168.1.10:8080/api/v1/cars"))
        assertEquals(
            "https://tm.example.com/teslamate",
            normalizeBaseUrl("https://tm.example.com/teslamate/api/v1/"),
        )
    }

    @Test
    fun rejectsMissingScheme() {
        assertNull(normalizeBaseUrl("192.168.1.10:8080"))
        assertNull(normalizeBaseUrl("ftp://192.168.1.10"))
        assertNull(normalizeBaseUrl("   "))
        assertNull(normalizeBaseUrl("http://"))
    }
}
