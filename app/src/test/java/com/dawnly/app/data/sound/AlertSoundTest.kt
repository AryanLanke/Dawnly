package com.dawnly.app.data.sound

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertSoundTest {
    @Test
    fun `nothing saved means the phone's own sound`() {
        assertEquals(AlertSound.PhoneDefault, AlertSound.fromKey(null))
    }

    @Test
    fun `a saved choice comes back`() {
        AlertSound.entries.forEach { assertEquals(it, AlertSound.fromKey(it.name)) }
    }

    @Test
    fun `a sound that no longer exists falls back to the phone's sound`() {
        assertEquals(AlertSound.PhoneDefault, AlertSound.fromKey("Gong"))
    }

    @Test
    fun `phone default comes first in the list`() {
        assertEquals(AlertSound.PhoneDefault, AlertSound.entries.first())
    }
}
