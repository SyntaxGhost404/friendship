package app.nearfolk.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.nearfolk.core.common.FuzzyMatcher
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfChoiceMark
import app.nearfolk.core.designsystem.component.NfChoiceRow
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfSearchField
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Countries
import app.nearfolk.core.model.ReportReason
import androidx.compose.ui.draw.clip

/**
 * Country chooser. Single select closes on tap; multi select collects a set
 * and applies it. [codes] limits the list, for example to countries present
 * on Discover.
 */
@Composable
fun CountryPickerSheet(
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    multiSelect: Boolean,
    codes: List<String> = Countries.codes,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf(selected) }
    val entries = remember(codes) {
        codes.map { it to Countries.displayName(it) }.sortedBy { it.second }
    }
    val shown = remember(query, entries) { FuzzyMatcher.rank(query, entries) { it.second } }
    NfBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.ui_country_title)) {
        Column(Modifier.fillMaxHeight(0.85f).imePadding()) {
            NfSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.ui_country_search),
                clearDescription = stringResource(R.string.ui_country_clear),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (multiSelect && query.isBlank()) {
                    item {
                        CountryRow(stringResource(R.string.ui_country_any), "", picked.isEmpty()) { picked = emptySet() }
                    }
                }
                items(shown, key = { it.first }) { (code, name) ->
                    CountryRow(name, Countries.flag(code), code in picked) {
                        if (multiSelect) {
                            picked = if (code in picked) picked - code else picked + code
                        } else {
                            onConfirm(setOf(code))
                        }
                    }
                }
            }
            if (multiSelect) {
                NfButton(
                    stringResource(R.string.ui_country_apply),
                    onClick = { onConfirm(picked) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CountryRow(name: String, flag: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(NfShapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(flag.ifEmpty { " " }, style = Nf.type.title)
        Text(name, style = Nf.type.body, color = Nf.colors.textPrimary, modifier = Modifier.weight(1f))
        NfChoiceMark(selected)
    }
}

fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.Harassment -> R.string.ui_report_reason_harassment
    ReportReason.Spam -> R.string.ui_report_reason_spam
    ReportReason.SexualContent -> R.string.ui_report_reason_sexual
    ReportReason.Hate -> R.string.ui_report_reason_hate
    ReportReason.Impersonation -> R.string.ui_report_reason_impersonation
    ReportReason.Underage -> R.string.ui_report_reason_underage
    ReportReason.SelfHarm -> R.string.ui_report_reason_selfharm
    ReportReason.Other -> R.string.ui_report_reason_other
}

@Composable
fun ReportSheet(
    memberName: String,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason, String?) -> Unit,
) {
    var reason by remember { mutableStateOf<ReportReason?>(null) }
    var details by rememberSaveable { mutableStateOf("") }
    NfBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.ui_report_title, memberName)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                stringResource(R.string.ui_report_body, memberName),
                style = Nf.type.bodySmall,
                color = Nf.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            ReportReason.entries.forEach { r ->
                NfChoiceRow(stringResource(r.labelRes()), selected = reason == r, onClick = { reason = r })
            }
            NfTextField(
                value = details,
                onValueChange = { details = it },
                label = stringResource(R.string.ui_report_details),
                placeholder = stringResource(R.string.ui_report_details_hint),
                singleLine = false,
                minLines = 3,
                maxLength = 500,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
            NfButton(
                stringResource(R.string.ui_report_submit),
                onClick = { reason?.let { onSubmit(it, details) } },
                enabled = reason != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun BlockDialog(memberName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    NfConfirmDialog(
        title = stringResource(R.string.ui_block_title, memberName),
        message = stringResource(R.string.ui_block_body),
        confirmLabel = stringResource(R.string.ui_block_confirm),
        dismissLabel = stringResource(R.string.ui_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
    )
}
