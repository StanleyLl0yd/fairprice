package com.sl.fairprice.parser

import java.math.BigDecimal

enum class PriceKind { REGULAR, CARD, REFERENCE }
enum class Measure { GRAM, MILLILITRE, PIECE }

data class PriceCandidate(
    val amountRub: BigDecimal,
    val kind: PriceKind,
    val source: String,
    val requiresConfirmation: Boolean
)
data class QuantityCandidate(
    val amount: BigDecimal,
    val measure: Measure,
    val source: String,
    val requiresConfirmation: Boolean
)
data class ParsedLabel(
    val prices: List<PriceCandidate>,
    val quantities: List<QuantityCandidate>,
    val needsReview: Boolean
)

/**
 * OCR-independent, conservative label interpretation.
 * A number may be a sale price, reference price, loyalty price, amount or
 * percentage. Ambiguous values must always be reviewed by the user.
 */
object PriceLabelParser {
    private val priceWithCurrency = Regex(
        """(?<!\d)(\d{1,7}(?:[ \u00A0\u202F]\d{3})*(?:[.,]\d{1,2})?)\s*(?:₽|руб\.?|р\.)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )
    private val barePrice = Regex(
        """(?<![\d.,])(\d{1,3}(?:[ \u00A0\u202F]\d{3})+[.,]\d{2}|\d{1,5}[.,]\d{2})(?!\d)"""
    )
    private val referenceUnit = Regex(
        """(?:цена\s+|стоимость\s+)?за\s*(?:1\s*)?(?:кг|килограмм(?:а|ов)?|л|литр(?:а|ов)?|шт\.?)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )
    private val loyalty = Regex(
        """(?:по\s+карте|с\s+картой|карта\s+магазина)""", RegexOption.IGNORE_CASE
    )
    private val discount = Regex(
        """(?:скидк\p{L}*|экономи\p{L}*|уценк\p{L}*)""", RegexOption.IGNORE_CASE
    )
    private val forwardMultipack = Regex(
        """(\d+)\s*(?:шт\.?\s*по\s*|[xх×*]\s*)(\d+(?:[.,]\d+)?)\s*(кг|гр?\.?|мл|л)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )
    private val reverseMultipack = Regex(
        """(\d+(?:[.,]\d+)?)\s*(кг|гр?\.?|мл|л)\s*[xх×*]\s*(\d+)(?![\d])""",
        RegexOption.IGNORE_CASE
    )
    private val unit = Regex(
        """(?<![\d.,])(\d+(?:[.,]\d+)?)\s*(кг|гр?\.?|мл|л|шт\.?)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )
    private val numberWithPieces = Regex(
        """(?<!\d)(\d+)\s*шт\.?(?![\p{L}\d])""", RegexOption.IGNORE_CASE
    )
    private val adjacentUnit = Regex(
        """^(?:кг|гр?\.?|г|мл|л|шт\.?)(?![\p{L}\d])""", RegexOption.IGNORE_CASE
    )

    private fun decimal(input: String): BigDecimal? =
        input.replace(" ", "").replace("\u00A0", "").replace("\u202F", "")
            .replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

    private fun clean(value: BigDecimal): BigDecimal =
        value.stripTrailingZeros().toPlainString().toBigDecimal()

    private fun metric(number: BigDecimal, u: String): Pair<BigDecimal, Measure>? =
        when (u.lowercase().trimEnd('.')) {
            "кг" -> clean(number.multiply(BigDecimal("1000"))) to Measure.GRAM
            "г", "гр" -> number to Measure.GRAM
            "л" -> clean(number.multiply(BigDecimal("1000"))) to Measure.MILLILITRE
            "мл" -> number to Measure.MILLILITRE
            "шт" -> number to Measure.PIECE
            else -> null
        }

    private fun overlaps(a: IntRange, b: IntRange): Boolean =
        a.first <= b.last && b.first <= a.last

    fun parse(text: String): ParsedLabel {
        val prices = mutableListOf<PriceCandidate>()
        val quantities = mutableListOf<QuantityCandidate>()
        for (line in text.lineSequence().map(String::trim).filter(String::isNotEmpty)) {
            val references = referenceUnit.findAll(line).map { it.range }.toList()
            val explicit = priceWithCurrency.findAll(line).toList()
            val matches = if (explicit.isNotEmpty()) explicit else barePrice.findAll(line).filter { m ->
                val after = line.substring(m.range.last + 1).trimStart()
                val before = line.substring(0, m.range.first).takeLast(20)
                !after.startsWith('%') &&
                    !adjacentUnit.containsMatchIn(after) &&
                    !discount.containsMatchIn(before)
            }.toList()

            matches.forEachIndexed { index, m ->
                val previousEnd = matches.getOrNull(index - 1)?.range?.last?.plus(1) ?: 0
                val before = line.substring(previousEnd, m.range.first)
                val after = if (index == matches.lastIndex) line.substring(m.range.last + 1) else ""
                val kind = when {
                    referenceUnit.containsMatchIn(before) -> PriceKind.REFERENCE
                    loyalty.containsMatchIn(before) || loyalty.containsMatchIn(after) -> PriceKind.CARD
                    else -> PriceKind.REGULAR
                }
                decimal(m.groupValues[1])?.let { amount ->
                    prices += PriceCandidate(
                        amount, kind, line,
                        explicit.isEmpty() || kind != PriceKind.REGULAR
                    )
                }
            }

            // Keep multipack ranges together, rather than emitting competing
            // "4 pieces" and "125 grams" for "4 pieces of 125 grams".
            val occupied = mutableListOf<IntRange>()
            for (m in forwardMultipack.findAll(line)) {
                val n = decimal(m.groupValues[1])
                val each = decimal(m.groupValues[2])
                val value = each?.let { metric(it, m.groupValues[3]) }
                if (n != null && value != null && references.none { overlaps(it, m.range) }) {
                    quantities += QuantityCandidate(
                        clean(n.multiply(value.first)), value.second, line, false
                    )
                    occupied += m.range
                }
            }
            for (m in reverseMultipack.findAll(line)) {
                if (occupied.any { overlaps(it, m.range) }) continue
                val each = decimal(m.groupValues[1])
                val n = decimal(m.groupValues[3])
                val value = each?.let { metric(it, m.groupValues[2]) }
                if (n != null && value != null && references.none { overlaps(it, m.range) }) {
                    quantities += QuantityCandidate(
                        clean(n.multiply(value.first)), value.second, line, false
                    )
                    occupied += m.range
                }
            }
            for (m in unit.findAll(line)) {
                if (occupied.any { overlaps(it, m.range) } ||
                    references.any { overlaps(it, m.range) }) continue
                val v = decimal(m.groupValues[1])?.let { metric(it, m.groupValues[2]) } ?: continue
                quantities += QuantityCandidate(v.first, v.second, line, false)
                occupied += m.range
            }
            for (m in numberWithPieces.findAll(line)) {
                if (occupied.any { overlaps(it, m.range) } ||
                    references.any { overlaps(it, m.range) }) continue
                decimal(m.groupValues[1])?.let {
                    quantities += QuantityCandidate(it, Measure.PIECE, line, false)
                }
            }
        }
        val distinctPrices = prices.distinctBy { Triple(it.amountRub, it.kind, it.source) }
        val distinctQuantities = quantities.distinctBy { Triple(it.amount, it.measure, it.source) }
        val available = distinctPrices.filter { it.kind != PriceKind.REFERENCE }
        return ParsedLabel(distinctPrices, distinctQuantities,
            needsReview = available.size != 1 || distinctQuantities.size != 1 ||
                distinctPrices.any { it.requiresConfirmation })
    }
}
