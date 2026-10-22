package com.sl.fairprice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sl.fairprice.R
import com.sl.fairprice.parser.Measure
import com.sl.fairprice.pricing.PriceEntry
import com.sl.fairprice.pricing.StandardUnit
import com.sl.fairprice.pricing.UnitPriceCalculator

/**
 * Transient draft. OCR only proposes values; every edit and newly selected photo
 * revokes confirmation until the user explicitly checks both fields again.
 */
data class OfferDraft(
    val price: String = "",
    val quantity: String = "",
    val measure: Measure = Measure.GRAM,
    val confirmed: Boolean = false,
    val ocrText: String = "",
    val ocrDetails: String = ""
)

@Composable
private fun unitLabel(measure: Measure): String = when (measure) {
    Measure.GRAM -> stringResource(R.string.unit_gram)
    Measure.MILLILITRE -> stringResource(R.string.unit_ml)
    Measure.PIECE -> stringResource(R.string.unit_piece)
}

@Composable
private fun standardUnitLabel(unit: StandardUnit): String = when (unit) {
    StandardUnit.KILOGRAM -> stringResource(R.string.unit_kilogram)
    StandardUnit.LITRE -> stringResource(R.string.unit_litre)
    StandardUnit.PIECE -> stringResource(R.string.unit_piece)
}

private fun localized(value: java.math.BigDecimal): String =
    value.toPlainString().replace('.', ',')

@Composable
private fun OfferEditor(
    number: Int,
    offer: OfferDraft,
    busy: Boolean,
    onEdit: (OfferDraft) -> Unit,
    onConfirm: (Boolean) -> Unit,
    onPhoto: () -> Unit,
    onCamera: () -> Unit
) {
    val valid = PriceEntry.offer(offer.price, offer.quantity, offer.measure) != null
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.offer_number, number), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = offer.price,
                    onValueChange = { onEdit(offer.copy(price = it)) },
                    label = { Text(stringResource(R.string.field_price)) },
                    modifier = Modifier.weight(1f).testTag("offer-$number-price"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = offer.quantity,
                    onValueChange = { onEdit(offer.copy(quantity = it)) },
                    label = { Text(stringResource(R.string.field_quantity)) },
                    modifier = Modifier.weight(1f).testTag("offer-$number-quantity"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (measure in Measure.entries) {
                    FilterChip(
                        selected = offer.measure == measure,
                        modifier = Modifier.testTag("offer-$number-measure-${measure.name}"),
                        onClick = { onEdit(offer.copy(measure = measure)) },
                        label = { Text(unitLabel(measure)) },
                        enabled = !busy
                    )
                }
            }
            Button(onClick = onPhoto, enabled = !busy) {
                Text(stringResource(R.string.photo_for_offer, number))
            }
            OutlinedButton(
                onClick = onCamera,
                enabled = !busy,
                modifier = Modifier.testTag("offer-$number-camera")
            ) {
                Text(stringResource(R.string.camera_for_offer, number))
            }
            if (offer.ocrText.isNotBlank()) {
                Text(stringResource(R.string.ocr_check_values), color = MaterialTheme.colorScheme.error)
                Text(offer.ocrDetails, style = MaterialTheme.typography.bodySmall)
                Text(offer.ocrText, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Checkbox(
                    checked = offer.confirmed,
                    modifier = Modifier.testTag("offer-$number-confirm"),
                    onCheckedChange = onConfirm,
                    enabled = !busy && valid
                )
                Text(
                    stringResource(R.string.confirm_offer),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!valid) {
                Text(stringResource(R.string.invalid_offer), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CompareScreen(
    offers: List<OfferDraft>,
    busy: Boolean,
    onEdit: (Int, OfferDraft) -> Unit,
    onConfirm: (Int, Boolean) -> Unit,
    onPhoto: (Int) -> Unit,
    onCamera: (Int) -> Unit
) {
    require(offers.size == 2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.comparison_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.comparison_description), style = MaterialTheme.typography.bodyMedium)
        offers.forEachIndexed { index, offer ->
            OfferEditor(
                number = index + 1,
                offer = offer,
                busy = busy,
                onEdit = { onEdit(index, it) },
                onConfirm = { onConfirm(index, it) },
                onPhoto = { onPhoto(index) },
                onCamera = { onCamera(index) }
            )
        }
        HorizontalDivider()
        Text(stringResource(R.string.comparison_result_title), style = MaterialTheme.typography.titleLarge)
        val first = PriceEntry.offer(offers[0].price, offers[0].quantity, offers[0].measure)
        val second = PriceEntry.offer(offers[1].price, offers[1].quantity, offers[1].measure)
        when {
            first == null || second == null ->
                Text(stringResource(R.string.comparison_enter_valid))
            !offers.all { it.confirmed } ->
                Text(stringResource(R.string.comparison_must_confirm))
            first.measure != second.measure ->
                Text(stringResource(R.string.comparison_incompatible),
                    modifier = Modifier.testTag("comparison-incompatible"),
                    color = MaterialTheme.colorScheme.error)
            else -> {
                val result = UnitPriceCalculator.compare(first, second)
                val unit = standardUnitLabel(result.first.standardUnit)
                Text(stringResource(R.string.unit_price_label, 1, localized(result.first.rub), unit))
                Text(stringResource(R.string.unit_price_label, 2, localized(result.second.rub), unit))
                if (result.cheaperIndex == null) {
                    Text(stringResource(R.string.comparison_tie), style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(stringResource(R.string.comparison_winner, result.cheaperIndex + 1),
                        modifier = Modifier.testTag("comparison-winner"),
                        style = MaterialTheme.typography.titleMedium)
                    val gap = if (result.savedRubPerUnit.signum() == 0)
                        stringResource(R.string.saving_under_kopeck)
                    else localized(result.savedRubPerUnit)
                    Text(stringResource(R.string.comparison_saving, gap, unit,
                        localized(result.savedPercentOfMoreExpensive)))
                }
            }
        }
    }
}
