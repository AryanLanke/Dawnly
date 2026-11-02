package com.dawnly.app.notify

/** Where the user lands once the Clock app has dismissed. Decided by [ReturnPlan]. */
sealed interface ReturnTo {
    data object Dawnly : ReturnTo
    data class App(val packageName: String) : ReturnTo
    data object Home : ReturnTo
}

/**
 * A dismiss leaves the phone the way the user left it (the user's rule, 2026-11-03): no Clock app
 * and no Dawnly popping up, just the app they were in. Pure, so it's unit tested.
 */
object ReturnPlan {
    /**
     * @param lastApp the app in front just before the dismiss, from Usage access; null if unknown.
     * @param homeApps the home screen and system UI: being there means going back home.
     */
    fun pick(lastApp: String?, ownPackage: String, homeApps: Set<String>): ReturnTo = when {
        lastApp == null || lastApp in homeApps -> ReturnTo.Home
        lastApp == ownPackage -> ReturnTo.Dawnly
        else -> ReturnTo.App(lastApp)
    }
}
