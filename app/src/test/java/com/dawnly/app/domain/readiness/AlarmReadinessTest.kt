package com.dawnly.app.domain.readiness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReadinessTest {
    private fun input(
        exactAlarms: Boolean = true,
        battery: Boolean = true,
        notifications: Boolean = true,
        alarmAudible: Boolean = true,
        fullScreen: Boolean = true,
    ) = ReadinessInput(exactAlarms, battery, notifications, alarmAudible, fullScreen)

    @Test fun `all checks passed is ready with nothing to fix`() {
        val result = AlarmReadiness.evaluate(input())
        assertTrue(result.ready)
        assertTrue(result.needsAttention.isEmpty())
        assertFalse(result.fullScreenOff)
    }

    @Test fun `each critical check alone blocks readiness`() {
        val cases = mapOf(
            ReadinessItem.ExactAlarms to input(exactAlarms = false),
            ReadinessItem.Battery to input(battery = false),
            ReadinessItem.Notifications to input(notifications = false),
            ReadinessItem.AlarmVolume to input(alarmAudible = false),
        )
        cases.forEach { (item, value) ->
            val result = AlarmReadiness.evaluate(value)
            assertFalse("expected $item to block readiness", result.ready)
            assertEquals(listOf(item), result.needsAttention)
        }
    }

    @Test fun `full screen off alone stays ready because the alarm rings as a notification`() {
        val result = AlarmReadiness.evaluate(input(fullScreen = false))
        assertTrue(result.ready)
        assertTrue(result.needsAttention.isEmpty())
        assertTrue(result.fullScreenOff)
    }

    @Test fun `missing checks are listed in settings order`() {
        val result = AlarmReadiness.evaluate(input(exactAlarms = false, notifications = false, alarmAudible = false))
        assertFalse(result.ready)
        assertEquals(
            listOf(ReadinessItem.ExactAlarms, ReadinessItem.Notifications, ReadinessItem.AlarmVolume),
            result.needsAttention,
        )
    }
}