package com.sl.fairprice.ocr

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sl.fairprice.camera.OcrImageLoader
import com.sl.fairprice.parser.Measure
import com.sl.fairprice.parser.PriceKind
import com.sl.fairprice.parser.PriceLabelParser
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Optional physical-device benchmark producer. Its data files are staged
 * explicitly into app-specific external files, NEVER bundled into APK or sent
 * to any server. Skips only when manifest is not staged.
 *
 * Not a release acceptance test without independently verified real photos.
 */
@RunWith(AndroidJUnit4::class)
class RussianCorpusExportTest {
    @Test fun exportPredictionsFromStagedPhotos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = context.getExternalFilesDir("ocr-benchmark")
            ?: error("Application-specific external files directory unavailable")
        val manifest = File(folder, "truth.jsonl")
        assumeTrue("No staged real-photo benchmark corpus; not a quality PASS", manifest.isFile)
        val root = folder.canonicalFile
        val output = File(root, "predictions.jsonl")
        val pending = File(root, "predictions.jsonl.part")
        // Never mistake a previous run for a successful current export.
        output.delete()
        pending.delete()
        var processed = 0
        val seen = mutableSetOf<String>()
        try {
            pending.bufferedWriter(Charsets.UTF_8).use { writer ->
                manifest.forEachLine(Charsets.UTF_8) { line ->
                    if (line.isNotBlank()) {
                        val record = JSONObject(line)
                        val id = record.getString("id")
                        require(id.isNotBlank() && seen.add(id)) {
                            "Empty/duplicate photo ID"
                        }
                        val relative = record.getString("image")
                        require(relative.isNotBlank() && !File(relative).isAbsolute &&
                                relative.replace('\\', '/').split('/').none { it == ".." }) {
                            "Unsafe relative corpus image path"
                        }
                        val image = File(root, relative).canonicalFile
                        require(image.path.startsWith(root.path + File.separator) && image.isFile) {
                            "Corpus image missing or outside app storage: $id"
                        }
                        val bitmap = OcrImageLoader.decode(context, Uri.fromFile(image))
                        val recognized = try {
                            TesseractOfflineOcr(context).recognize(bitmap)
                        } finally {
                            bitmap.recycle()
                        }
                        val parsed = PriceLabelParser.parse(recognized.text)
                        val eligible = parsed.prices.filter { it.kind != PriceKind.REFERENCE }
                        val definite = !parsed.needsReview &&
                            eligible.size == 1 && parsed.quantities.size == 1
                        val prediction = JSONObject()
                            .put("id", id)
                            .put("ambiguous", !definite)
                            .put("elapsed_ms", recognized.elapsedMs)
                            .put("ocr_confidence", recognized.meanConfidence)
                        if (definite) {
                            val quantity = parsed.quantities.single()
                            prediction.put("price_rub", eligible.single().amountRub.toPlainString())
                            prediction.put("quantity", quantity.amount.toPlainString())
                            prediction.put("measure", when (quantity.measure) {
                                Measure.GRAM -> "g"
                                Measure.MILLILITRE -> "ml"
                                Measure.PIECE -> "piece"
                            })
                        }
                        writer.append(prediction.toString()).append('\n')
                        processed++
                    }
                }
            }
            assertTrue("Empty corpus (no photos processed)", processed > 0)
            check(pending.renameTo(output)) { "Cannot commit predictions JSONL" }
        } finally {
            pending.delete()
        }
    }
}
