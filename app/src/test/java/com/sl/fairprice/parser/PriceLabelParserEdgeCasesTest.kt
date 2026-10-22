package com.sl.fairprice.parser

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

/** Synthetic text regressions; NOT a benchmark on real photographed labels. */
class PriceLabelParserEdgeCasesTest {
    private fun parse(text: String) = PriceLabelParser.parse(text)

    @Test fun packageMassSurvivesReferencePriceOnSameLine() {
        val p = parse("Сыр 200 г - 179,90 ₽; цена за 1 кг - 899,50 ₽")
        assertEquals(BigDecimal("200"), p.quantities.single().amount)
        assertEquals(listOf(PriceKind.REGULAR, PriceKind.REFERENCE), p.prices.map { it.kind })
        assertTrue(p.needsReview)
    }

    @Test fun referencePriceDoesNotGenerateFakeQuantity() {
        val p = parse("Цена за 1 кг - 899,50 ₽")
        assertEquals(PriceKind.REFERENCE, p.prices.single().kind)
        assertTrue(p.quantities.isEmpty())
        assertTrue(p.needsReview)
    }

    @Test fun referenceAndPackageAtOppositeEndsOfLine() {
        val p = parse("Цена за 1 кг 899 ₽; упаковка 200 г")
        assertEquals(BigDecimal("200"), p.quantities.single().amount)
        assertEquals(PriceKind.REFERENCE, p.prices.single().kind)
    }

    @Test fun forwardAndReverseMultipacksProduceOneTotal() {
        assertEquals(BigDecimal("500"), parse("Йогурт 4 шт. по 125 г - 219,90 ₽").quantities.single().amount)
        assertEquals(BigDecimal("500"), parse("Йогурт 125 г × 4 - 219,90 ₽").quantities.single().amount)
        assertEquals(BigDecimal("1000"), parse("2 x 500 г - 180 ₽").quantities.single().amount)
        assertEquals(BigDecimal("1000"), parse("2 х 500 г - 180 ₽").quantities.single().amount)
        assertEquals(BigDecimal("1200"), parse("6 шт. по 200 мл - 299 ₽").quantities.single().amount)
    }

    @Test fun fractionAndUnitPriceNeverUsePercentageAsMoney() {
        val p = parse("Молоко 3,20% 900 мл 129,99")
        assertEquals(BigDecimal("129.99"), p.prices.single().amountRub)
        assertEquals(BigDecimal("900"), p.quantities.single().amount)
        assertTrue(p.prices.single().requiresConfirmation)
        assertTrue(p.needsReview)
    }

    @Test fun unmarkedDecimalQuantityIsNotATentativePrice() {
        assertTrue(parse("Масса 1,50 кг").prices.isEmpty())
        assertTrue(parse("Объём 0,50 л").prices.isEmpty())
    }

    @Test fun groupSeparatorsAndBarePriceAreHandledConservatively() {
        assertEquals(BigDecimal("1299.90"), parse("Кофе 1\u202f299,90 ₽").prices.single().amountRub)
        val bare = parse("Цена 1 299,99\nМасса 500 г")
        assertEquals(BigDecimal("1299.99"), bare.prices.single().amountRub)
        assertTrue(bare.needsReview)
    }

    @Test fun trailingCardQualifierRequiresReview() {
        val p = parse("149,90 ₽ по карте\n500 г")
        assertEquals(PriceKind.CARD, p.prices.single().kind)
        assertTrue(p.needsReview)
    }

    @Test fun countAndReferenceAreDifferentTypes() {
        assertEquals(BigDecimal("12"), parse("Салфетки 12 шт. 99 ₽").quantities.single().amount)
        assertTrue(parse("Цена за 1 шт - 55 ₽").quantities.isEmpty())
    }

    @Test fun missingOrMultipleValuesRequireReview() {
        assertTrue(parse("Сыр 299,90 ₽").needsReview)
        assertTrue(parse("199,90 ₽\n149,90 ₽\n500 г").needsReview)
        assertTrue(parse("По карте 149,99 ₽\n200 г").needsReview)
    }

    @Test fun invalidValuesCannotBecomeCandidates() {
        assertTrue(parse("Скидка 20%").prices.isEmpty())
        assertTrue(parse("0 ₽ 0 г").prices.isEmpty())
        assertTrue(parse("0 ₽ 0 г").quantities.isEmpty())
    }
}
