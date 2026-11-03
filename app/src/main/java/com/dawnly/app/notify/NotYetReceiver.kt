package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dawnly.app.dawnly
import com.dawnly.app.domain.learn.Answer
import java.time.Instant
import kotlinx.coroutines.launch

/**
 * The prompt's "Not yet" button: just closes it, and stops an auto-dismiss countdown. Swiping the
 * countdown away lands here too. The alarm rings as normal, and the user isn't asked again this
 * window, since wake detection reports at most once per window.
 */
class NotYetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AwakeNotifier.cancel(context)
        // Asked too early: Dawnly learns to wait for a little more next time.
        val pending = goAsync()
        val app = context.dawnly
        app.scope.launch {
            try {
                app.learning.answer(Answer.NotYet, Instant.now())
            } finally {
                pending.finish()
            }
        }
    }
}
