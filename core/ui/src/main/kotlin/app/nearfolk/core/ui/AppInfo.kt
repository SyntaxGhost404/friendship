package app.nearfolk.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Build details the app module knows and feature modules need. */
data class AppInfo(val versionName: String, val supportEmail: String)

val LocalAppInfo = staticCompositionLocalOf { AppInfo(versionName = "", supportEmail = "") }
