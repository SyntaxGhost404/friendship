package app.nearfolk.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalTextStyle

val LocalNfColors = staticCompositionLocalOf { GraphiteColors }
val LocalNfTypography = staticCompositionLocalOf { NfTypography() }

/** Entry point for theme tokens: `Nf.colors`, `Nf.type`, `Nf.shapes`. */
object Nf {
    val colors: NfColors
        @Composable @ReadOnlyComposable get() = LocalNfColors.current
    val type: NfTypography
        @Composable @ReadOnlyComposable get() = LocalNfTypography.current
    val shapes: NfShapes get() = NfShapes
}

fun nfColors(dark: Boolean, pureBlack: Boolean): NfColors = when {
    !dark -> PaperColors
    pureBlack -> GraphiteBlackColors
    else -> GraphiteColors
}

@Composable
fun NearfolkTheme(
    darkTheme: Boolean,
    pureBlack: Boolean = false,
    haptics: AppHaptics = AppHaptics.None,
    content: @Composable () -> Unit,
) {
    val colors = nfColors(darkTheme, pureBlack)
    val type = remember { NfTypography() }
    val scheme = remember(colors) { colors.toMaterial() }
    val materialType = remember(type) { type.toMaterial() }
    MaterialTheme(
        colorScheme = scheme,
        typography = materialType,
    ) {
        CompositionLocalProvider(
            LocalNfColors provides colors,
            LocalNfTypography provides type,
            LocalAppHaptics provides haptics,
            LocalContentColor provides colors.textPrimary,
            LocalTextStyle provides type.body,
            LocalIndication provides ripple(color = colors.textPrimary),
            content = content,
        )
    }
}

private fun NfColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = onAccentSoft,
        inversePrimary = accent,
        secondary = textSecondary,
        onSecondary = canvas,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = textPrimary,
        tertiary = online,
        onTertiary = canvas,
        tertiaryContainer = surfaceRaised,
        onTertiaryContainer = textPrimary,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = textSecondary,
        surfaceTint = canvas,
        inverseSurface = ink,
        inverseOnSurface = onInk,
        error = danger,
        onError = onDanger,
        errorContainer = dangerSoft,
        onErrorContainer = onDangerSoft,
        outline = lineStrong,
        outlineVariant = line,
        scrim = scrim,
        surfaceBright = surfaceRaised,
        surfaceDim = canvas,
        surfaceContainerLowest = surfaceSunken,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
    )
}

private fun NfTypography.toMaterial() = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = headline,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = title,
    titleLarge = title,
    titleMedium = titleSmall,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = bodySmall,
    labelLarge = label,
    labelMedium = labelSmall,
    labelSmall = caption,
)
