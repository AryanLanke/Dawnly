package com.dawnly.app.ui.settings

import com.dawnly.app.domain.wake.WakeSignalSettings
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivityTest {
    @Test
    fun `the default goals are medium`() {
        assertEquals(Sensitivity.Medium, Sensitivity.of(WakeSignalSettings()))
    }

    @Test
    fun `applying a level sets both goals and keeps the switches`() {
        val settings = WakeSignalSettings(useUnlock = false, useSteps = true)
        val high = Sensitivity.High.applyTo(settings)
        assertEquals(Duration.ofMinutes(3), high.screenTimeGoal)
        assertEquals(30, high.stepsGoal)
        assertFalse(high.useUnlock)
        assertTrue(high.useSteps)
        assertEquals(Sensitivity.High, Sensitivity.of(high))
    }

    @Test
    fun `goals that match no level select none`() {
        assertNull(Sensitivity.of(WakeSignalSettings(screenTimeGoal = Duration.ofMinutes(7))))
    }

    @Test
    fun `the last sign can't be turned off`() {
        val onlyUnlock = WakeSignalSettings(useUnlock = true, useScreenTime = false, useSteps = false)
        assertNull(onlyUnlock.withSign(Sign.Unlock, false))
        assertEquals(
            onlyUnlock.copy(useSteps = true),
            onlyUnlock.withSign(Sign.Steps, true),
        )
    }

    @Test
    fun `other signs turn off freely`() {
        val updated = WakeSignalSettings().withSign(Sign.ScreenTime, false)
        assertEquals(WakeSignalSettings(useScreenTime = false), updated)
    }
}
