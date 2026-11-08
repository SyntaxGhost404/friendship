package app.nearfolk.core.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Date picker styled with the app tokens. Dates outside [min]..[max] cannot be picked. */
@Composable
fun NfDatePickerDialog(
    initial: LocalDate,
    min: LocalDate,
    max: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val minMillis = min.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val maxMillis = max.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.coerceIn(min, max).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        yearRange = min.year..max.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in minMillis..maxMillis
            override fun isSelectableYear(year: Int) = year in min.year..max.year
        },
    )
    val c = Nf.colors
    val colors = DatePickerDefaults.colors(
        containerColor = c.surface,
        titleContentColor = c.textSecondary,
        headlineContentColor = c.textPrimary,
        weekdayContentColor = c.textTertiary,
        subheadContentColor = c.textSecondary,
        yearContentColor = c.textPrimary,
        currentYearContentColor = c.accent,
        selectedYearContentColor = c.onAccent,
        selectedYearContainerColor = c.accent,
        dayContentColor = c.textPrimary,
        selectedDayContentColor = c.onAccent,
        selectedDayContainerColor = c.accent,
        todayContentColor = c.accent,
        todayDateBorderColor = c.accent,
        navigationContentColor = c.textPrimary,
        dividerColor = c.line,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = NfShapes.extraLarge,
        colors = colors,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text(stringResource(R.string.ui_done), color = c.textPrimary, style = Nf.type.label) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel), color = c.textSecondary, style = Nf.type.label) }
        },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = true)
    }
}
