package app.nearfolk.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens.
 *
 * The palette is graphite and paper neutrals with a single straw accent
 * (hue near 46 degrees). Chat bubbles stay monochrome so long conversations
 * remain easy on the eyes; the accent is saved for actions and unread state.
 */
@Immutable
data class NfColors(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    /** Raised above surfaceRaised, for thumbs and selected segments. */
    val surfaceTop: Color,
    val line: Color,
    val lineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val ink: Color,
    val onInk: Color,
    val online: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerSoft: Color,
    val onDangerSoft: Color,
    val bubbleMine: Color,
    val onBubbleMine: Color,
    val bubbleTheirs: Color,
    val onBubbleTheirs: Color,
    val scrim: Color,
)

internal val GraphiteColors = NfColors(
    isDark = true,
    canvas = Color(0xFF111210),
    surface = Color(0xFF191A17),
    surfaceRaised = Color(0xFF22241F),
    surfaceSunken = Color(0xFF0C0D0B),
    surfaceTop = Color(0xFF33352F),
    line = Color(0xFF2C2E29),
    lineStrong = Color(0xFF3E4139),
    textPrimary = Color(0xFFEDEBE3),
    textSecondary = Color(0xFFA9A79C),
    textTertiary = Color(0xFF77766C),
    accent = Color(0xFFE2C25E),
    onAccent = Color(0xFF1B1704),
    accentSoft = Color(0xFF2F2A15),
    onAccentSoft = Color(0xFFEBD48C),
    ink = Color(0xFFEDEBE3),
    onInk = Color(0xFF151613),
    online = Color(0xFF63C27D),
    danger = Color(0xFFD9564B),
    onDanger = Color(0xFF1C0705),
    dangerSoft = Color(0xFF3A1A15),
    onDangerSoft = Color(0xFFF2B3A9),
    bubbleMine = Color(0xFFE6E3D8),
    onBubbleMine = Color(0xFF161713),
    bubbleTheirs = Color(0xFF262823),
    onBubbleTheirs = Color(0xFFEDEBE3),
    scrim = Color(0x99000000),
)

/** OLED variant: true black canvas, everything else nudged down to match. */
internal val GraphiteBlackColors = GraphiteColors.copy(
    canvas = Color(0xFF000000),
    surface = Color(0xFF0E0F0D),
    surfaceRaised = Color(0xFF181916),
    surfaceSunken = Color(0xFF000000),
    surfaceTop = Color(0xFF2A2C27),
    line = Color(0xFF23251F),
    lineStrong = Color(0xFF34372F),
    bubbleTheirs = Color(0xFF1B1D19),
)

internal val PaperColors = NfColors(
    isDark = false,
    canvas = Color(0xFFF5F4EE),
    surface = Color(0xFFFCFBF7),
    surfaceRaised = Color(0xFFECEBE4),
    surfaceSunken = Color(0xFFE5E3DA),
    surfaceTop = Color(0xFFFFFFFF),
    line = Color(0xFFDEDCD2),
    lineStrong = Color(0xFFC8C5B9),
    textPrimary = Color(0xFF181915),
    textSecondary = Color(0xFF5B5B52),
    textTertiary = Color(0xFF85847A),
    accent = Color(0xFF7A6210),
    onAccent = Color(0xFFFFFDF4),
    accentSoft = Color(0xFFF0E6C2),
    onAccentSoft = Color(0xFF453705),
    ink = Color(0xFF1C1D19),
    onInk = Color(0xFFF5F4EE),
    online = Color(0xFF2E8C4C),
    danger = Color(0xFFB4372A),
    onDanger = Color(0xFFFFF8F6),
    dangerSoft = Color(0xFFF6DDD8),
    onDangerSoft = Color(0xFF5E150D),
    bubbleMine = Color(0xFF1F201C),
    onBubbleMine = Color(0xFFF5F4EE),
    bubbleTheirs = Color(0xFFE8E6DE),
    onBubbleTheirs = Color(0xFF181915),
    scrim = Color(0x66000000),
)

/** Earthy tones for generated avatars and artwork. No banned hues. */
object NfArtTones {
    val moss = Color(0xFF5E6B3A)
    val sage = Color(0xFF9AAB8A)
    val pine = Color(0xFF2F4A35)
    val olive = Color(0xFF7D7A3A)
    val straw = Color(0xFFD9BC62)
    val sand = Color(0xFFD8CBA8)
    val bone = Color(0xFFEAE4D3)
    val clay = Color(0xFF8E4B3B)
    val umber = Color(0xFF5A4433)
    val charcoal = Color(0xFF2B2C28)
    val lichen = Color(0xFFB7B98A)
    val fern = Color(0xFF4F7A4F)

    val all = listOf(moss, sage, pine, olive, straw, sand, bone, clay, umber, charcoal, lichen, fern)
}
