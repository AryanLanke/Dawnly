package com.dawnly.app.notify

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReturnPlanTest {
    private val own = "com.dawnly.app"
    private val home = setOf("com.nothing.launcher", "com.android.systemui")

    @Test
    fun `goes back to the app the user was in`() {
        assertEquals(ReturnTo.App("com.spotify.music"), ReturnPlan.pick("com.spotify.music", own, home))
    }

    @Test
    fun `stays in Dawnly when the user was in Dawnly`() {
        assertEquals(ReturnTo.Dawnly, ReturnPlan.pick(own, own, home))
    }

    @Test
    fun `goes home from the home screen or the system UI`() {
        assertEquals(ReturnTo.Home, ReturnPlan.pick("com.nothing.launcher", own, home))
        assertEquals(ReturnTo.Home, ReturnPlan.pick("com.android.systemui", own, home))
    }

    @Test
    fun `goes home when it can't tell, e g without Usage access`() {
        assertEquals(ReturnTo.Home, ReturnPlan.pick(null, own, home))
    }

    @Test
    fun `the dismissed notice clears itself only when the phone is unlocked`() {
        assertNull(DismissedNotice.timeout(locked = true))
        assertEquals(Duration.ofMinutes(1), DismissedNotice.timeout(locked = false))
    }
}
