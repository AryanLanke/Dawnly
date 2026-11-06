package com.dawnly.app.service

import android.content.BroadcastReceiver
import android.content.Context
import com.dawnly.app.dawnly
import kotlinx.coroutines.launch

/**
 * Runs [block] on the app's scope while keeping this receiver's process alive (`goAsync`), and
 * tells Android it's done when [block] ends, however it ends.
 */
fun BroadcastReceiver.launchAsync(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.dawnly.scope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
