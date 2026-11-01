package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * The prompt's "Not yet" button: just closes it, and stops an auto-dismiss countdown. Swiping the
 * countdown away lands here too. The alarm rings as normal, and the user isn't asked again this
 * window, since wake detection reports at most once per window.
 */
class NotYetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AwakeNotifier.cancel(context)
    }
}
