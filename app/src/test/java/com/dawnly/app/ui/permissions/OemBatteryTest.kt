package com.dawnly.app.ui.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OemBatteryTest {
    @Test
    fun `detects the brands that need extra steps`() {
        assertEquals(OemBattery.Samsung, OemBattery.detect("samsung"))
        assertEquals(OemBattery.Xiaomi, OemBattery.detect("Xiaomi"))
        assertEquals(OemBattery.Xiaomi, OemBattery.detect("Redmi"))
        assertEquals(OemBattery.Xiaomi, OemBattery.detect(" POCO "))
    }

    @Test
    fun `other brands need nothing extra`() {
        assertNull(OemBattery.detect("Nothing"))
        assertNull(OemBattery.detect("Google"))
        assertNull(OemBattery.detect(""))
    }
}
