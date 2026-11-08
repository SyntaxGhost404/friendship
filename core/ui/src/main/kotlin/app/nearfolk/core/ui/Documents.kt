package app.nearfolk.core.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.Nf

enum class AppDocument(@StringRes val title: Int, @ArrayRes val body: Int) {
    Guidelines(R.string.doc_guidelines_title, R.array.doc_guidelines),
    Privacy(R.string.doc_privacy_title, R.array.doc_privacy),
    Terms(R.string.doc_terms_title, R.array.doc_terms),
    Licenses(R.string.doc_licenses_title, R.array.doc_licenses),
}

/** Renders a document array where lines starting with # are headings. */
@Composable
fun DocumentBody(document: AppDocument, modifier: Modifier = Modifier) {
    val lines = stringArrayResource(document.body)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            if (line.startsWith("#")) {
                Text(line.removePrefix("#"), style = Nf.type.titleSmall, color = Nf.colors.textPrimary, modifier = Modifier.padding(top = 14.dp))
            } else {
                Text(line, style = Nf.type.body, color = Nf.colors.textSecondary)
            }
        }
    }
}
