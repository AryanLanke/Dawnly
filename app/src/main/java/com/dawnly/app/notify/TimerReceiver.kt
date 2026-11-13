package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Manifest receiver must be exported=false, in the app's default process (no intent filter). */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerController.ACTION_EXPIRED || !intent.hasExtra(TimerController.EXTRA_GENERATION)) return
        TimerController.receive(context, intent.getLongExtra(TimerController.EXTRA_GENERATION, -1L))
    }
}
