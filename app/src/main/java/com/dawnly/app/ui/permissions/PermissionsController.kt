package com.dawnly.app.ui.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Which permissions are granted right now. Ones that don't exist on this Android count as granted. */
data class PermissionState(private val granted: Set<AppPermission>) {
    operator fun contains(permission: AppPermission) = permission in granted

    companion object {
        val AllGranted = PermissionState(AppPermission.entries.toSet())

        fun read(context: Context) =
            PermissionState(AppPermission.entries.filter { it.isGranted(context) }.toSet())
    }
}

/**
 * Asks for permissions and keeps [state] current. Runtime permissions show a system dialog.
 * Exact alarms and battery are granted on a system screen instead, so they're checked again when
 * the user comes back to Dawnly.
 */
@Stable
class PermissionsController internal constructor(
    /** An activity, since this starts system screens. */
    private val context: Context,
) {
    var state by mutableStateOf(PermissionState.read(context))
        private set

    internal lateinit var launcher: ManagedActivityResultLauncher<String, Boolean>
    private var pending: Pending? = null

    private class Pending(val permission: AppPermission, val onResult: (Boolean) -> Unit) {
        val startedAt = SystemClock.elapsedRealtime()

        /** True once we've sent the user to a system screen; the answer comes on resume. */
        var inSettings = false
    }

    /** Asks for [permission]. [onResult] gets whether it ended up granted. */
    fun request(permission: AppPermission, onResult: (Boolean) -> Unit = {}) {
        if (permission.isGranted(context)) {
            onResult(true)
            return
        }
        val request = Pending(permission, onResult)
        pending = request
        val runtime = permission.runtimePermission
        if (runtime != null) launcher.launch(runtime) else openSettings(request)
    }

    fun refresh() {
        state = PermissionState.read(context)
    }

    /** Opens the brand's own battery screen, or the closest one this phone has. */
    fun openBatterySettings(oem: OemBattery) {
        oem.intents(context).firstOrNull { tryStart(it) }
    }

    internal fun onRuntimeResult(granted: Boolean) {
        val request = pending ?: return
        refresh()
        // Once a permission has been denied twice, Android stops showing the dialog and answers
        // "no" straight away. An instant "no" means the user never saw a dialog: go to settings.
        val instant = SystemClock.elapsedRealtime() - request.startedAt < INSTANT_DENIAL_MS
        if (!granted && instant) {
            openSettings(request)
        } else {
            finish(request)
        }
    }

    internal fun onResume() {
        refresh()
        // A runtime dialog pauses and resumes us too, but its answer comes through the launcher.
        pending?.takeIf { it.inSettings }?.let(::finish)
    }

    private fun openSettings(request: Pending) {
        val intent = request.permission.settingsIntent(context)
        val fallback = AppPermission.appDetails("package:${context.packageName}".toUri())
        request.inSettings = tryStart(intent) || tryStart(fallback)
        if (!request.inSettings) finish(request)
    }

    private fun finish(request: Pending) {
        if (pending === request) pending = null
        request.onResult(request.permission in state)
    }

    private fun tryStart(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.i(TAG, "Not on this phone: $intent")
        false
    } catch (e: SecurityException) {
        Log.i(TAG, "Not allowed to open $intent")
        false
    }

    private companion object {
        const val TAG = "Permissions"
        const val INSTANT_DENIAL_MS = 400L
    }
}

/** A [PermissionsController] tied to this screen's activity. Not usable in previews. */
@Composable
fun rememberPermissionsController(): PermissionsController {
    val context = LocalContext.current
    val controller = remember(context) { PermissionsController(context) }
    controller.launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        controller.onRuntimeResult(it)
    }
    LifecycleResumeEffect(controller) {
        controller.onResume()
        onPauseOrDispose {}
    }
    return controller
}
