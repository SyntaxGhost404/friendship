package app.nearfolk.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Mood
import app.nearfolk.core.model.ZodiacSign
import java.time.LocalDate

@StringRes
fun Gender.labelRes(): Int = when (this) {
    Gender.Female -> R.string.ui_gender_female
    Gender.Male -> R.string.ui_gender_male
    Gender.Nonbinary -> R.string.ui_gender_nonbinary
}

@StringRes
fun ZodiacSign.labelRes(): Int = when (this) {
    ZodiacSign.Aries -> R.string.ui_zodiac_aries
    ZodiacSign.Taurus -> R.string.ui_zodiac_taurus
    ZodiacSign.Gemini -> R.string.ui_zodiac_gemini
    ZodiacSign.Cancer -> R.string.ui_zodiac_cancer
    ZodiacSign.Leo -> R.string.ui_zodiac_leo
    ZodiacSign.Virgo -> R.string.ui_zodiac_virgo
    ZodiacSign.Libra -> R.string.ui_zodiac_libra
    ZodiacSign.Scorpio -> R.string.ui_zodiac_scorpio
    ZodiacSign.Sagittarius -> R.string.ui_zodiac_sagittarius
    ZodiacSign.Capricorn -> R.string.ui_zodiac_capricorn
    ZodiacSign.Aquarius -> R.string.ui_zodiac_aquarius
    ZodiacSign.Pisces -> R.string.ui_zodiac_pisces
}

@StringRes
fun Mood.labelRes(): Int = when (this) {
    Mood.Calm -> R.string.ui_mood_calm
    Mood.Hopeful -> R.string.ui_mood_hopeful
    Mood.Grateful -> R.string.ui_mood_grateful
    Mood.Low -> R.string.ui_mood_low
    Mood.Restless -> R.string.ui_mood_restless
    Mood.Tired -> R.string.ui_mood_tired
}

/** "27 · Female · Leo" style summary. */
@Composable
fun Member.metaLine(today: LocalDate = LocalDate.now()): String =
    listOf(ageOn(today).toString(), stringResource(gender.labelRes()), stringResource(zodiac.labelRes())).joinToString("  ·  ")
