package com.dawnly.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.theme.DawnlyTheme

/** Where feedback goes: the developer's inbox (the user's choice, 2026-11-03). */
private const val FEEDBACK_EMAIL = "redischarmender@gmail.com"
private const val GMAIL = "com.google.android.gm"

/** The Settings row that opens Gmail with a feedback email ready to write. */
@Composable
internal fun FeedbackRow() {
    val context = LocalContext.current
    GlassList {
        GlassRow(
            title = stringResource(R.string.settings_feedback),
            subtitle = stringResource(R.string.settings_feedback_detail),
            modifier = Modifier.clickable(role = Role.Button) { openFeedback(context) },
        ) {
            Icon(
                GlassIcons.Chevron,
                contentDescription = null,
                tint = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 6.dp).size(18.dp),
            )
        }
    }
}

/**
 * A new email to [FEEDBACK_EMAIL] in Gmail, or in whatever email app the phone has. The app version
 * and phone go at the bottom, so a report says which phone it's about.
 */
private fun openFeedback(context: Context) {
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
    val about = context.getString(
        R.string.feedback_about,
        version.orEmpty(),
        "${Build.MANUFACTURER} ${Build.MODEL}",
        Build.VERSION.RELEASE,
    )
    val email = Intent(Intent.ACTION_SENDTO, "mailto:$FEEDBACK_EMAIL".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject))
        .putExtra(Intent.EXTRA_TEXT, "\n\n$about")
    val opened = listOf(Intent(email).setPackage(GMAIL), email).any { intent ->
        try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
    if (!opened) Toast.makeText(context, context.getString(R.string.feedback_no_app, FEEDBACK_EMAIL), Toast.LENGTH_LONG).show()
}
