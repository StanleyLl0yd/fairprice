package com.sl.fairprice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sl.fairprice.R
import com.sl.fairprice.history.SavedComparison
import com.sl.fairprice.parser.Measure
import java.text.DateFormat
import java.util.Date

private fun textUnit(measure: Measure): String = when(measure) {
    Measure.GRAM -> "г"
    Measure.MILLILITRE -> "мл"
    Measure.PIECE -> "шт."
}

@Composable
fun HistorySection(
    rows: List<SavedComparison>,
    canSave: Boolean,
    busy: Boolean,
    onSave: () -> Unit,
    onRestore: (SavedComparison) -> Unit,
    onDelete: (Long) -> Unit
) {
    HorizontalDivider()
    Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.history_description), style = MaterialTheme.typography.bodySmall)
    Button(onClick = onSave, enabled = canSave && !busy,
        modifier = Modifier.testTag("history-save")) {
        Text(stringResource(R.string.history_save))
    }
    if (rows.isEmpty()) {
        Text(stringResource(R.string.history_empty))
    }
    for (row in rows) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(row.createdAtMillis)))
                Text(stringResource(R.string.history_offer_pair,
                    row.first.priceRub.toPlainString(), row.first.quantity.toPlainString(),
                    row.second.priceRub.toPlainString(), row.second.quantity.toPlainString(),
                    textUnit(row.first.measure)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onRestore(row) }, enabled = !busy,
                        modifier = Modifier.testTag("history-restore-${row.id}")) {
                        Text(stringResource(R.string.history_restore))
                    }
                    OutlinedButton(onClick = { onDelete(row.id) }, enabled = !busy,
                        modifier = Modifier.testTag("history-delete-${row.id}")) {
                        Text(stringResource(R.string.history_delete))
                    }
                }
            }
        }
    }
}
