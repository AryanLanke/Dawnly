package com.dawnly.app.ui.time

/** Round a countdown UP, so a fresh five-second timer never immediately reads four. */
internal fun timerText(milliseconds: Long): String {
    val ms = milliseconds.coerceAtLeast(0L)
    return clockText(ms / 1000 + if (ms % 1000 == 0L) 0 else 1)
}

internal fun stopwatchText(milliseconds: Long): String {
    val ms = milliseconds.coerceAtLeast(0L)
    return clockText(ms / 1000) + "." + (ms % 1000 / 10).toString().padStart(2, '0')
}

private fun clockText(seconds: Long): String =
    listOf(seconds / 3600, seconds / 60 % 60, seconds % 60).joinToString(":") { it.toString().padStart(2, '0') }
