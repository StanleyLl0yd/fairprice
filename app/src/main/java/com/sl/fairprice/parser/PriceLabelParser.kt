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

/** OCR-independent and conservative. No invented measurements. */
object PriceLabelParser {
    private val priceWithCurrency =
        Regex("""(?<![\d])(\d{1,7}(?:[ \u00A0]\d{3})*(?:[.,]\d{1,2})?)\s*(?:₽|руб\.?|р\.)""", RegexOption.IGNORE_CASE)
    private val barePrice =
        Regex("""(?<![\d.,])(\d{1,5}[.,]\d{2})(?!\d)""")
    private val unitPrice =
        Regex("""(?:цена\s+)?за\s*(?:1\s*)?(?:кг|килограмм|л|литр|шт)(?![\p{L}\d])""", RegexOption.IGNORE_CASE)
    private val loyalty =
        Regex("""(?:по\s+карте|с\s+картой|карта\s+магазина)""", RegexOption.IGNORE_CASE)
    private val multiple =
        Regex("""(\d+)\s*(?:шт\.?\s*по|[xх×*])\s*(\d+(?:[.,]\d+)?)\s*(кг|гр?\.?|г|мл|л)(?![\p{L}\d])""", RegexOption.IGNORE_CASE)
    private val unit =
        Regex("""(?<![\d])(\d+(?:[.,]\d+)?)\s*(кг|гр?\.?|г|мл|л|шт\.?)(?![\p{L}\d])""", RegexOption.IGNORE_CASE)
    private val count = Regex("""(?<![\d])(\d+)\s*шт\.?""", RegexOption.IGNORE_CASE)

    private fun decimal(input: String): BigDecimal? =
        input.replace(" ", "").replace("\u00A0", "").replace(',', '.')
            .toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

    private fun clean(number: BigDecimal): BigDecimal =
        number.stripTrailingZeros().toPlainString().toBigDecimal()

    private fun metric(number: BigDecimal, unit: String): Pair<BigDecimal, Measure>? =
        when (unit.lowercase().trimEnd('.')) {
            "кг" -> clean(number.multiply(BigDecimal("1000"))) to Measure.GRAM
            "г", "гр" -> number to Measure.GRAM
            "л" -> clean(number.multiply(BigDecimal("1000"))) to Measure.MILLILITRE
            "мл" -> number to Measure.MILLILITRE
            "шт" -> number to Measure.PIECE
            else -> null
        }

    fun parse(text: String): ParsedLabel {
        val prices = mutableListOf<PriceCandidate>()
        val quantities = mutableListOf<QuantityCandidate>()
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        for (line in lines) {
            val reference = unitPrice.containsMatchIn(line)
            val explicit = priceWithCurrency.findAll(line).toList()
            val matches = if (explicit.isNotEmpty()) explicit else {
                barePrice.findAll(line).filterNot {
                    line.contains("%") || line.contains("скидк", ignoreCase = true)
                }.toList()
            }
            for (m in matches) {
                val previous = matches.lastOrNull { it.range.last < m.range.first }
                val contextStart = (previous?.range?.last?.plus(1)) ?: 0
                val prefix = line.substring(contextStart, m.range.first)
                val kind = when {
                    unitPrice.containsMatchIn(prefix) -> PriceKind.REFERENCE
                    loyalty.containsMatchIn(prefix) -> PriceKind.CARD
                    else -> PriceKind.REGULAR
                }
                decimal(m.groupValues[1])?.let {
                    prices += PriceCandidate(it, kind, line, explicit.isEmpty() || kind != PriceKind.REGULAR)
                }
            }
            val multi = multiple.find(line)
            if (multi != null) {
                val n = decimal(multi.groupValues[1])
                val one = decimal(multi.groupValues[2])
                val measurement = one?.let { metric(it, multi.groupValues[3]) }
                if (n != null && measurement != null) {
                    quantities += QuantityCandidate(
                        clean(n.multiply(measurement.first)), measurement.second, line, false)
                }
            } else if (!reference) {
                for (m in unit.findAll(line)) {
                    val v = decimal(m.groupValues[1]) ?: continue
                    val measurement = metric(v, m.groupValues[2]) ?: continue
                    quantities += QuantityCandidate(measurement.first, measurement.second, line, false)
                }
                if (quantities.none { it.source == line && it.measure == Measure.PIECE }) {
                    count.find(line)?.let { c ->
                        decimal(c.groupValues[1])?.let {
                            quantities += QuantityCandidate(it, Measure.PIECE, line, false)
                        }
                    }
                }
            }
        }
        val distinctPrices = prices.distinctBy { Triple(it.amountRub, it.kind, it.source) }
        val distinctQty = quantities.distinctBy { Triple(it.amount, it.measure, it.source) }
        val salePrices = distinctPrices.filter { it.kind != PriceKind.REFERENCE }
        return ParsedLabel(distinctPrices, distinctQty,
            salePrices.size != 1 || distinctQty.size != 1 ||
            distinctPrices.any { it.requiresConfirmation })
    }
}
