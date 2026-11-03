package com.dawnly.app.ui.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
 * The rest are granted on a system screen instead, so they're checked again when the user comes
 * back to Dawnly.
 *
 * Some phones (Xiaomi above all) accept a screen's intent and show nothing, or close it at once.
 * Then the next screen in [AppPermission.settingsIntents] is tried, ending at App info, so a tap
 * on Allow always opens something.
 */
@Stable
class PermissionsController internal constructor(
    /** An activity, since this starts system screens. */
    private val context: Context,
) {
    var state by mutableStateOf(PermissionState.read(context))
        private set

    /**
     * Set when Android's restricted settings lock (Android 13+, apps installed from a file) is the
     * likely reason a permission wasn't turned on. Shows [RestrictedSettingsHelp].
     */
    var restrictedHelp by mutableStateOf<AppPermission?>(null)
        private set

    internal lateinit var launcher: ManagedActivityResultLauncher<String, Boolean>
    private var pending: Pending? = null
    private val handler = Handler(Looper.getMainLooper())

    /** Failed tries per lockable permission, so the help shows by the second at the latest. */
    private val failedTries = mutableMapOf<AppPermission, Int>()

    private class Pending(val permission: AppPermission, val onResult: (Boolean) -> Unit) {
        val startedAt = SystemClock.elapsedRealtime()

        /** True once we've sent the user to a system screen; the answer comes on resume. */
        var inSettings = false

        /** The screens still to try if the current one doesn't show. */
        var nextScreens: List<Intent> = emptyList()

        /** When the current screen was started, and whether Dawnly has gone behind it since. */
        var screenStartedAt = 0L
        var paused = false
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

    /** Opens Dawnly's App info page, where ⋮ > Allow restricted settings lives. */
    fun openAppInfo() {
        restrictedHelp = null
        tryStart(AppPermission.appDetails(context))
    }

    fun closeRestrictedHelp() {
        restrictedHelp = null
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
        val request = pending?.takeIf { it.inSettings } ?: return
        val shownFor = SystemClock.elapsedRealtime() - request.screenStartedAt
        if (request.permission !in state && shownFor < BOUNCE_MS && request.nextScreens.isNotEmpty()) {
            // Back before anyone could have read it: the screen closed by itself.
            Log.i(TAG, "${request.permission} screen closed at once; trying the next one")
            openNextScreen(request)
        } else {
            finish(request)
        }
    }

    internal fun onPause() {
        pending?.paused = true
    }

    private fun openSettings(request: Pending) {
        request.nextScreens = request.permission.settingsIntents(context)
        openNextScreen(request)
    }

    private fun openNextScreen(request: Pending) {
        while (request.nextScreens.isNotEmpty()) {
            val intent = request.nextScreens.first()
            request.nextScreens = request.nextScreens.drop(1)
            request.paused = false
            if (!tryStart(intent)) continue
            request.inSettings = true
            val startedAt = SystemClock.elapsedRealtime()
            request.screenStartedAt = startedAt
            // If Dawnly is still in front a moment later, nothing opened.
            handler.postDelayed({
                if (pending === request && request.screenStartedAt == startedAt && !request.paused) {
                    Log.i(TAG, "Nothing showed for $intent; trying the next screen")
                    openNextScreen(request)
                }
            }, SHOW_TIMEOUT_MS)
            return
        }
        finish(request)
    }

    private fun finish(request: Pending) {
        if (pending === request) pending = null
        val granted = request.permission in state
        if (!granted && request.inSettings && request.permission.canBeLocked) {
            val tries = (failedTries[request.permission] ?: 0) + 1
            failedTries[request.permission] = tries
            if (tries >= 2 || AppPermission.restrictedSettingsLocked(context) == true) {
                restrictedHelp = request.permission
            }
        }
        request.onResult(granted)
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

        /** A system screen gone again sooner than this closed by itself. */
        const val BOUNCE_MS = 600L

        /** A screen that hasn't covered Dawnly by now never will. */
        const val SHOW_TIMEOUT_MS = 1_500L
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
        onPauseOrDispose { controller.onPause() }
    }
    return controller
}
