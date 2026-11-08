package app.nearfolk.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.nearfolk.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
private fun instrument(weight: Int) = Font(
    resId = R.font.instrument_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Instrument Sans, variable, licensed under the SIL Open Font License 1.1. */
val InstrumentSans = FontFamily(
    instrument(400),
    instrument(500),
    instrument(600),
    instrument(700),
)

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = trim,
)

@Immutable
data class NfTypography(
    val display: TextStyle = style(44, 48, 600, -0.02),
    val headline: TextStyle = style(28, 34, 600, -0.012),
    val title: TextStyle = style(20, 26, 600, -0.006),
    val titleSmall: TextStyle = style(16, 22, 600),
    val body: TextStyle = style(15, 22, 400),
    val bodyStrong: TextStyle = style(15, 22, 500),
    val bodySmall: TextStyle = style(13, 18, 400),
    val label: TextStyle = style(14, 18, 500),
    val labelSmall: TextStyle = style(12, 16, 500, 0.01),
    val caption: TextStyle = style(11, 14, 500, 0.02),
)
