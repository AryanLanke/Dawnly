package com.dawnly.app.data.alarm

import android.content.Context

/**
 * Which form of the dismiss request ([DismissRequest]) last worked with each Clock app, so it's
 * sent first next time. Plain SharedPreferences: it's read on the main thread, right as the
 * request goes out.
 */
class DismissMemory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("clock_dismiss", Context.MODE_PRIVATE)

    fun worked(clockPackage: String): DismissRequest? =
        prefs.getString(clockPackage, null)?.let { name -> DismissRequest.entries.firstOrNull { it.name == name } }

    fun remember(clockPackage: String, request: DismissRequest) {
        prefs.edit().putString(clockPackage, request.name).apply()
    }
}
