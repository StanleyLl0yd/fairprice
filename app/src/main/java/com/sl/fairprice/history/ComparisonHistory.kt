package com.sl.fairprice.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sl.fairprice.parser.Measure
import com.sl.fairprice.pricing.ProductOffer
import com.sl.fairprice.pricing.UnitPriceCalculator
import java.math.BigDecimal

data class SavedComparison(
    val id: Long,
    val createdAtMillis: Long,
    val first: ProductOffer,
    val second: ProductOffer
)

/** Numbers only: no photos, raw OCR, text, analytics, or network dependencies. */
class ComparisonHistory(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "fairprice_comparisons.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE comparisons (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            created_at INTEGER NOT NULL,
            price_a TEXT NOT NULL,
            quantity_a TEXT NOT NULL,
            price_b TEXT NOT NULL,
            quantity_b TEXT NOT NULL,
            measure TEXT NOT NULL
        )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Never erase personal history as a fallback when schemas evolve.
        error("History migration from $oldVersion to $newVersion is not implemented")
    }

    fun save(first: ProductOffer, second: ProductOffer): Long {
        // Defensive validation even if the caller has an accidental UI bug.
        UnitPriceCalculator.compare(first, second)
        val values = ContentValues().apply {
            put("created_at", System.currentTimeMillis())
            put("price_a", first.priceRub.toPlainString())
            put("quantity_a", first.quantity.toPlainString())
            put("price_b", second.priceRub.toPlainString())
            put("quantity_b", second.quantity.toPlainString())
            put("measure", first.measure.name)
        }
        return writableDatabase.let { db ->
            db.beginTransaction()
            try {
                val id = db.insertOrThrow("comparisons", null, values)
                db.execSQL("""DELETE FROM comparisons WHERE id NOT IN (
                    SELECT id FROM comparisons ORDER BY id DESC LIMIT 50
                )""")
                db.setTransactionSuccessful()
                id
            } finally {
                db.endTransaction()
            }
        }
    }

    fun list(): List<SavedComparison> =
        readableDatabase.rawQuery(
            """SELECT id, created_at, price_a, quantity_a, price_b,
                quantity_b, measure FROM comparisons ORDER BY id DESC LIMIT 50""", null
        ).use { cursor ->
            val result = mutableListOf<SavedComparison>()
            while (cursor.moveToNext()) {
                val measure = Measure.valueOf(cursor.getString(6))
                val first = ProductOffer(
                    BigDecimal(cursor.getString(2)), BigDecimal(cursor.getString(3)), measure
                )
                val second = ProductOffer(
                    BigDecimal(cursor.getString(4)), BigDecimal(cursor.getString(5)), measure
                )
                result += SavedComparison(cursor.getLong(0), cursor.getLong(1), first, second)
            }
            result
        }

    fun delete(id: Long): Boolean =
        writableDatabase.delete("comparisons", "id = ?", arrayOf(id.toString())) == 1

    fun clear(): Int = writableDatabase.delete("comparisons", null, null)
}
