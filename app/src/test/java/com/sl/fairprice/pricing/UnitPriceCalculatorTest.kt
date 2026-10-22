package com.sl.fairprice.pricing

import com.sl.fairprice.parser.Measure
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class UnitPriceCalculatorTest {
    private fun offer(price: String, quantity: String, measure: Measure = Measure.GRAM) =
        ProductOffer(BigDecimal(price), BigDecimal(quantity), measure)

    @Test fun gramAndKilogramNormalization() {
        assertEquals(BigDecimal("399.80"), UnitPriceCalculator.quote(offer("199.90", "500")).rub)
        assertEquals(StandardUnit.KILOGRAM, UnitPriceCalculator.quote(offer("199.90", "500")).standardUnit)
        assertEquals(BigDecimal("199.90"), UnitPriceCalculator.quote(offer("199.90", "1000")).rub)
    }

    @Test fun litreFromMillilitres() {
        val q = UnitPriceCalculator.quote(offer("129.99", "900", Measure.MILLILITRE))
        assertEquals(StandardUnit.LITRE, q.standardUnit)
        assertEquals(BigDecimal("144.43"), q.rub)
    }

    @Test fun itemsAreNotMultipliedByThousand() {
        val q = UnitPriceCalculator.quote(offer("119.99", "4", Measure.PIECE))
        assertEquals(StandardUnit.PIECE, q.standardUnit)
        assertEquals(BigDecimal("30.00"), q.rub)
    }

    @Test fun winnerUsesExactMathNotRoundedUnitPrice() {
        val compared = UnitPriceCalculator.compare(offer("10.00", "1000"), offer("10.004", "1000"))
        assertEquals(0, compared.cheaperIndex)
        assertEquals(BigDecimal("10.00"), compared.first.rub)
        assertEquals(BigDecimal("10.00"), compared.second.rub)
    }

    @Test fun differenceAndPercentAreRelativeToMoreExpensive() {
        val compared = UnitPriceCalculator.compare(offer("180", "600"), offer("200", "500"))
        assertEquals(0, compared.cheaperIndex)
        assertEquals(BigDecimal("300.00"), compared.first.rub)
        assertEquals(BigDecimal("400.00"), compared.second.rub)
        assertEquals(BigDecimal("100.00"), compared.savedRubPerUnit)
        assertEquals(BigDecimal("25.00"), compared.savedPercentOfMoreExpensive)
    }

    @Test fun reversedOrderGivesSameSavings() {
        val c = UnitPriceCalculator.compare(offer("200", "500"), offer("180", "600"))
        assertEquals(1, c.cheaperIndex)
        assertEquals(BigDecimal("100.00"), c.savedRubPerUnit)
        assertEquals(BigDecimal("25.00"), c.savedPercentOfMoreExpensive)
    }

    @Test fun exactTieEvenWhenQuantityDiffers() {
        val c = UnitPriceCalculator.compare(offer("100", "250"), offer("200", "500"))
        assertNull(c.cheaperIndex)
        assertEquals(BigDecimal("0.00"), c.savedRubPerUnit)
        assertEquals(BigDecimal("0.00"), c.savedPercentOfMoreExpensive)
    }

    @Test fun noMixOfWeightVolumeAndPieces() {
        assertThrows(IllegalArgumentException::class.java) {
            UnitPriceCalculator.compare(offer("100", "500"), offer("100", "500", Measure.MILLILITRE))
        }
        assertThrows(IllegalArgumentException::class.java) {
            UnitPriceCalculator.compare(offer("100", "500"), offer("100", "2", Measure.PIECE))
        }
    }

    @Test fun nonPositiveAmountsRejected() {
        assertThrows(IllegalArgumentException::class.java) { offer("0", "100") }
        assertThrows(IllegalArgumentException::class.java) { offer("-1", "100") }
        assertThrows(IllegalArgumentException::class.java) { offer("10", "0") }
        assertThrows(IllegalArgumentException::class.java) { offer("10", "-2") }
    }

    @Test fun strictManualInputSupportsRussianDecimalAndGrouping() {
        assertEquals(BigDecimal("1299.99"), PriceEntry.positiveDecimal("1 299,99", 2))
        assertEquals(BigDecimal("1299.99"), PriceEntry.positiveDecimal("1\u00a0299,99", 2))
        assertEquals(BigDecimal("1299.99"), PriceEntry.positiveDecimal("1\u202f299,99", 2))
        assertEquals(BigDecimal("0.125"), PriceEntry.positiveDecimal("0,125", 3))
        assertEquals(BigDecimal("12.50"), PriceEntry.positiveDecimal("12.50", 2))
    }

    @Test fun rejectsMisgroupedAndUnsafeEntries() {
        for (s in listOf("", " ", "0", "-1", "1 2", "12 34", "1,234.56", "1e9", "1+2", "NaN", "1/2", "0,00")) {
            assertNull("Should reject: $s", PriceEntry.positiveDecimal(s))
        }
        assertNull(PriceEntry.positiveDecimal("129,999", 2))
    }

    @Test fun rejectsMoneyPrecisionAboveKopeck() {
        assertNull(PriceEntry.offer("12,345", "500", Measure.GRAM))
        assertNotNull(PriceEntry.offer("12,34", "500", Measure.GRAM))
        assertNull(PriceEntry.offer("12,34", "500,1234", Measure.GRAM))
    }

    @Test fun roundingUsesHalfUpRubleKopecks() {
        assertEquals(BigDecimal("0.13"), UnitPriceCalculator.quote(offer("1", "8", Measure.PIECE)).rub)
        assertEquals(BigDecimal("333.33"), UnitPriceCalculator.quote(offer("1", "3", Measure.GRAM)).rub)
    }
}
