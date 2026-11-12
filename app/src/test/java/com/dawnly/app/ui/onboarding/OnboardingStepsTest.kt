package com.dawnly.app.ui.onboarding

import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStepsTest {
    private val nothingGranted = PermissionState(emptySet())

    @Test
    fun `steps and usage access wait for Settings`() {
        val steps = onboardingSteps(nothingGranted, OemBattery.Xiaomi)
        assertFalse(AppPermission.ActivityRecognition in steps)
        assertFalse(AppPermission.UsageAccess in steps)
    }

    @Test
    fun `battery step only on brand phones that stop apps`() {
        assertFalse(AppPermission.Battery in onboardingSteps(nothingGranted, null))
        assertTrue(AppPermission.Battery in onboardingSteps(PermissionState.AllGranted, OemBattery.Samsung))
    }

    @Test
    fun `nothing to walk through once everything is allowed on a plain phone`() {
        assertEquals(emptyList<AppPermission>(), onboardingSteps(PermissionState.AllGranted, null))
    }
}
