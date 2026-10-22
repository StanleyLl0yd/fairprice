package com.sl.fairprice.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import com.sl.fairprice.model.OcrResult
import com.sl.fairprice.parser.Measure
import com.sl.fairprice.parser.PriceLabelParser
import java.math.BigDecimal

/** Entirely local synthetic diagnostic; never reads, stores or uploads user photographs. */
data class OcrSelfTestReport(
    val passed: Boolean,
    val diagnostic: String,
    val recognized: OcrResult,
)

object OcrSelfTest {
    fun run(context: Context): OcrSelfTestReport {
        val bitmap = Bitmap.createBitmap(1800, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 90f
            isFakeBoldText = true
        }
        canvas.drawText("Молоко 900 мл", 60f, 160f, paint)
        canvas.drawText("129,99 руб.", 60f, 310f, paint)
        val result = try {
            TesseractOfflineOcr(context).recognize(bitmap)
        } finally {
            bitmap.recycle()
        }
        val parsed = PriceLabelParser.parse(result.text)
        val cyrillic = result.text.any { it in 'А'..'я' }
        val price = parsed.prices.any {
            it.amountRub.compareTo(BigDecimal("129.99")) == 0
        }
        val quantity = parsed.quantities.any {
            it.measure == Measure.MILLILITRE &&
                it.amount.compareTo(BigDecimal("900")) == 0
        }
        val boxes = result.words.isNotEmpty() && result.words.all {
            it.box.right > it.box.left && it.box.bottom > it.box.top
        }
        val confidence = result.meanConfidence in 0..100
        val passed = cyrillic && price && quantity && boxes && confidence
        val diagnostic = buildString {
            appendLine("FairPrice OCR preview 0.0.1 - synthetic self-test")
            appendLine("Overall: " + (if (passed) "PASS" else "FAIL"))
            appendLine("Android API: " + Build.VERSION.SDK_INT)
            appendLine("ABI: " + Build.SUPPORTED_ABIS.joinToString(","))
            appendLine("Cyrillic: " + cyrillic)
            appendLine("Price 129.99 RUB: " + price)
            appendLine("Quantity 900 ml: " + quantity)
            appendLine("Valid word boxes: " + boxes)
            appendLine("Confidence in range: " + confidence)
            appendLine("OCR confidence: " + result.meanConfidence)
            appendLine("OCR elapsed ms: " + result.elapsedMs)
            appendLine("Recognized synthetic text: " +
                result.text.trim().replace('\n', ' ').take(300))
        }
        return OcrSelfTestReport(passed, diagnostic, result)
    }
}
