package com.dawnly.app.notify

import com.dawnly.app.data.settings.WakeMusicApp
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeMusicPlanTest {
    private fun tries(
        choice: WakeMusicApp = WakeMusicApp.Spotify,
        online: Boolean = true,
        installed: Boolean = true,
        alreadyPlaying: Boolean = false,
        canDismiss: Boolean = true,
    ) = WakeMusicPlan.shouldTry(choice, online, installed, alreadyPlaying, canDismiss)

    @Test
    fun `starts ahead of the alarm`() {
        val alarmAt = Instant.parse("2026-11-12T06:30:00Z")
        assertEquals(Instant.parse("2026-11-12T06:29:15Z"), WakeMusicPlan.startAt(alarmAt))
    }

    @Test
    fun `tries only when it can work`() {
        assertTrue(tries())
        assertTrue(tries(choice = WakeMusicApp.AnyApp))
        assertFalse(tries(choice = WakeMusicApp.Off))
        assertFalse(tries(online = false))
        assertFalse(tries(installed = false))
        assertFalse(tries(alreadyPlaying = true))
        assertFalse(tries(canDismiss = false))
    }

    @Test
    fun `test results explain why playback was skipped`() {
        assertEquals(MusicResult.Off, WakeMusicPlan.blockedBy(WakeMusicApp.Off, true, true, false, true))
        assertEquals(MusicResult.Offline, WakeMusicPlan.blockedBy(WakeMusicApp.Spotify, false, true, false, true))
        assertEquals(MusicResult.NotInstalled, WakeMusicPlan.blockedBy(WakeMusicApp.Spotify, true, false, false, true))
        assertEquals(MusicResult.AlreadyPlaying, WakeMusicPlan.blockedBy(WakeMusicApp.Spotify, true, true, true, true))
        assertEquals(MusicResult.CannotDismiss, WakeMusicPlan.blockedBy(WakeMusicApp.Spotify, true, true, false, false))
        assertEquals(null, WakeMusicPlan.blockedBy(WakeMusicApp.AnyApp, true, true, false, true))
    }

    @Test
    fun `raises a quiet phone to half volume`() {
        assertEquals(8, WakeMusicPlan.minVolume(15))
        assertEquals(13, WakeMusicPlan.minVolume(25))
    }
}
