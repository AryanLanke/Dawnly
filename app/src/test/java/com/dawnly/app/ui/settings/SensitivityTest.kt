package com.dawnly.app.ui.settings

import com.dawnly.app.domain.wake.Sensitivity
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
    fun `applying a level sets every goal and keeps the switches`() {
        val settings = WakeSignalSettings(useMusic = false, useSteps = true)
        val high = Sensitivity.High.applyTo(settings)
        assertEquals(Duration.ofMinutes(3), high.screenTimeGoal)
        assertEquals(2, high.appsOpenedGoal)
        assertEquals(30, high.stepsGoal)
        assertEquals(Duration.ofMinutes(1), high.movementGoal)
        assertFalse(high.useMusic)
        assertTrue(high.useSteps)
        assertEquals(Sensitivity.High, Sensitivity.of(high))
    }

    @Test
    fun `goals that match no level select none`() {
        assertNull(Sensitivity.of(WakeSignalSettings(screenTimeGoal = Duration.ofMinutes(7))))
    }

    @Test
    fun `the last sign can't be turned off`() {
        val onlySteps = WakeSignalSettings(useScreenTime = false, useSteps = true, useMovement = false, useMusic = false)
        assertNull(onlySteps.withSign(Sign.Steps, false))
        assertEquals(onlySteps.copy(useMovement = true), onlySteps.withSign(Sign.Movement, true))
    }

    @Test
    fun `music alone isn't enough to keep`() {
        val movementAndMusic = WakeSignalSettings(useScreenTime = false, useMovement = true, useMusic = true)
        assertNull(movementAndMusic.withSign(Sign.Movement, false))
        assertEquals(movementAndMusic.copy(useMusic = false), movementAndMusic.withSign(Sign.Music, false))
    }

    @Test
    fun `other signs turn off freely`() {
        val updated = WakeSignalSettings().withSign(Sign.ScreenTime, false)
        assertEquals(WakeSignalSettings(useScreenTime = false), updated)
    }
}
