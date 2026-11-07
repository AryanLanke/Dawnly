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

/** Where feedback goes: a new issue on the public project page. */
private const val FEEDBACK_URL = "https://github.com/AryanLanke/Dawnly/issues/new"

/** The Settings row that opens a new GitHub issue with the app version and phone filled in. */
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
 * A new GitHub issue in the browser, with the app version and phone in the body, so a report says
 * which phone it's about.
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
    val url = FEEDBACK_URL.toUri().buildUpon()
        .appendQueryParameter("title", context.getString(R.string.feedback_subject))
        .appendQueryParameter("body", "\n\n$about")
        .build()
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.feedback_no_app, FEEDBACK_URL), Toast.LENGTH_LONG).show()
    }
}
