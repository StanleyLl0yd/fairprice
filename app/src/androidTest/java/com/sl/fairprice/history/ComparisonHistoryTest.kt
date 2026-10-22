package com.sl.fairprice.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sl.fairprice.parser.Measure
import com.sl.fairprice.pricing.ProductOffer
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComparisonHistoryTest {
    private fun offer(p:String,q:String,m:Measure=Measure.GRAM) =
        ProductOffer(BigDecimal(p), BigDecimal(q), m)

    @Test fun persistsNumbersOnlyAndDeleteIsSelective() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = ComparisonHistory(ctx)
        db.clear()
        try {
            val id = db.save(offer("129.99","900"),offer("199.99","1000"))
            assertTrue(id > 0)
            val rows = db.list()
            assertEquals(1, rows.size)
            assertEquals(BigDecimal("129.99"), rows.single().first.priceRub)
            assertEquals(Measure.GRAM, rows.single().second.measure)
            assertTrue(db.delete(id))
            assertTrue(db.list().isEmpty())
            assertFalse(db.delete(id))
        } finally { db.clear(); db.close() }
    }

    @Test fun rejectsDifferentMeasuresWithoutSaving() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db=ComparisonHistory(ctx)
        db.clear()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                db.save(offer("100","500"),offer("200","750",Measure.MILLILITRE))
            }
            assertTrue(db.list().isEmpty())
        } finally { db.clear(); db.close() }
    }

    @Test fun retainsOnlyFiftyNewestRecords() {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val db=ComparisonHistory(ctx)
        db.clear()
        try {
            repeat(54) { index ->
                db.save(offer((100+index).toString(),"500"),offer("200","900"))
            }
            val rows=db.list()
            assertEquals(50, rows.size)
            assertEquals(BigDecimal("153"), rows.first().first.priceRub)
            assertEquals(BigDecimal("104"), rows.last().first.priceRub)
        } finally { db.clear(); db.close() }
    }
}
