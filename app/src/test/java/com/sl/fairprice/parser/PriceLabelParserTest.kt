package com.sl.fairprice.parser

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class PriceLabelParserTest {
 @Test fun russianMilk() {
  val p=PriceLabelParser.parse("Молоко 3,2% 900 мл - 129,99 руб.")
  assertEquals(BigDecimal("129.99"),p.prices.single().amountRub)
  assertEquals(BigDecimal("900"),p.quantities.single().amount)
  assertEquals(Measure.MILLILITRE,p.quantities.single().measure)
  assertFalse(p.needsReview)
 }
 @Test fun referencePriceIsNotPackagePrice() {
  val p=PriceLabelParser.parse("Цена за 1 кг - 899,50 ₽")
  assertEquals(PriceKind.REFERENCE,p.prices.single().kind)
  assertTrue(p.quantities.isEmpty())
  assertTrue(p.needsReview)
 }
 @Test fun loyaltyAndRegularNotConfused() {
  val p=PriceLabelParser.parse("Цена 199,99 ₽ По карте 149,99 ₽\n500 г")
  assertEquals(2,p.prices.size)
  assertEquals(PriceKind.REGULAR,p.prices.first().kind)
  assertEquals(PriceKind.CARD,p.prices.last().kind)
  assertTrue(p.needsReview)
 }
 @Test fun multipliedQuantity() {
  val p=PriceLabelParser.parse("Йогурт 4 шт. по 125 г - 219,90 ₽")
  assertEquals(BigDecimal("500"),p.quantities.single().amount)
 }
 @Test fun noInventedPrice() { assertTrue(PriceLabelParser.parse("Сыр 45% 200 г").prices.isEmpty()) }
 @Test fun decimalWeight() {
  assertEquals(BigDecimal("1500"),PriceLabelParser.parse("Сахар 1,5 кг - 99 руб.").quantities.single().amount)
 }
 @Test fun noDiscountAsPrice() { assertTrue(PriceLabelParser.parse("Скидка 20%").prices.isEmpty()) }
 @Test fun zeroRejected() {
  assertTrue(PriceLabelParser.parse("0 ₽ 0 г").prices.isEmpty())
  assertTrue(PriceLabelParser.parse("0 ₽ 0 г").quantities.isEmpty())
 }
}
