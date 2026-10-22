package com.sl.fairprice.pricing

import com.sl.fairprice.parser.Measure
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Input values are in RUB and the parser's base quantity units:
 * grams, millilitres, or individual pieces.
 * Nothing here depends on OCR confidence or Android runtime.
 */
data class ProductOffer(
    val priceRub: BigDecimal,
    val quantity: BigDecimal,
    val measure: Measure
) {
    init {
        require(priceRub.signum() > 0) { "Price must be positive" }
        require(quantity.signum() > 0) { "Quantity must be positive" }
    }
}

enum class StandardUnit { KILOGRAM, LITRE, PIECE }

data class UnitPrice(
    val rub: BigDecimal,
    val standardUnit: StandardUnit
)

/**
 * Cheaper offer is determined with exact cross-multiplication, before any
 * displayed monetary rounding. 0 = first, 1 = second, null = exact tie.
 */
data class Comparison(
    val first: UnitPrice,
    val second: UnitPrice,
    val cheaperIndex: Int?,
    val savedRubPerUnit: BigDecimal,
    val savedPercentOfMoreExpensive: BigDecimal
)

object UnitPriceCalculator {
    private val thousand = BigDecimal("1000")
    private val hundred = BigDecimal("100")

    fun standardUnit(measure: Measure): StandardUnit = when (measure) {
        Measure.GRAM -> StandardUnit.KILOGRAM
        Measure.MILLILITRE -> StandardUnit.LITRE
        Measure.PIECE -> StandardUnit.PIECE
    }

    private fun factor(measure: Measure): BigDecimal = when (measure) {
        Measure.GRAM, Measure.MILLILITRE -> thousand
        Measure.PIECE -> BigDecimal.ONE
    }

    fun quote(offer: ProductOffer): UnitPrice =
        UnitPrice(
            offer.priceRub.multiply(factor(offer.measure))
                .divide(offer.quantity, 2, RoundingMode.HALF_UP),
            standardUnit(offer.measure)
        )

    fun compare(first: ProductOffer, second: ProductOffer): Comparison {
        require(first.measure == second.measure) {
            "Mass, volume and piece-count offers must not be compared"
        }
        // Both offers have the same dimension. Positive quantities make
        // exact cross-products equivalent to their unrounded per-unit rates.
        val a = first.priceRub.multiply(second.quantity)
        val b = second.priceRub.multiply(first.quantity)
        val order = a.compareTo(b)
        val winner = when {
            order < 0 -> 0
            order > 0 -> 1
            else -> null
        }
        val gap = a.subtract(b).abs()
        val denominator = first.quantity.multiply(second.quantity)
        val perUnitGap = gap.multiply(factor(first.measure))
            .divide(denominator, 2, RoundingMode.HALF_UP)
        val moreExpensive = if (order >= 0) a else b
        val percentage = if (order == 0) BigDecimal.ZERO.setScale(2) else
            gap.multiply(hundred).divide(moreExpensive, 2, RoundingMode.HALF_UP)
        return Comparison(quote(first), quote(second), winner, perUnitGap, percentage)
    }
}

/**
 * Strict, predictable interpretation of manually confirmed numeric fields.
 * A comma is accepted as a decimal separator; internal spaces (incl. NBSP)
 * are accepted only as correctly grouped thousands separators.
 */
object PriceEntry {
    private val decimalPattern = Regex(
        """(?:\d+|\d{1,3}(?:[ \u00a0\u202f]\d{3})+)(?:[.,]\d+)?"""
    )

    fun positiveDecimal(raw: String, maxFractionDigits: Int = 3): BigDecimal? {
        require(maxFractionDigits in 0..9)
        val text = raw.trim()
        if (text.isEmpty() || text.length > 32 || !decimalPattern.matches(text)) return null
        val mark = text.lastIndexOfAny(charArrayOf('.', ','))
        if (mark >= 0 && text.length - mark - 1 > maxFractionDigits) return null
        val digits = text.replace(" ", "").replace("\u00a0", "")
            .replace("\u202f", "").replace(',', '.')
        return digits.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    }

    fun offer(price: String, quantity: String, measure: Measure): ProductOffer? {
        val p = positiveDecimal(price, maxFractionDigits = 2) ?: return null
        val q = positiveDecimal(quantity, maxFractionDigits = 3) ?: return null
        return ProductOffer(p, q, measure)
    }
}
