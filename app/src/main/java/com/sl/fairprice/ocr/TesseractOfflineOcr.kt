package com.sl.fairprice.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.sl.fairprice.model.OcrResult
import com.sl.fairprice.model.OcrWord
import com.sl.fairprice.model.TextBox
import java.io.File
import java.security.MessageDigest

/** Offline ARM64 Android baseline. The model is packaged in APK assets. */
class TesseractOfflineOcr(private val context: Context) : OfflineOcr {
 override fun recognize(bitmap: Bitmap): OcrResult {
  val start = android.os.SystemClock.elapsedRealtime()
  val base = File(context.filesDir, "tesseract")
  val data = File(base, "tessdata")
  check(data.exists() || data.mkdirs()) { "Cannot create OCR data directory" }
  val trained = File(data, "rus.traineddata")
  if (!verifyModel(trained)) {
   val temp = File(data, "rus.traineddata.part")
   try {
    context.assets.open("ocr/rus.traineddata").use { input ->
     temp.outputStream().use { input.copyTo(it) }
    }
    check(verifyModel(temp)) { "Invalid offline model SHA-256" }
    check(temp.renameTo(trained)) { "Could not install offline OCR model" }
   } finally { temp.delete() }
  }
  val api = TessBaseAPI()
  try {
   check(api.init(base.absolutePath, "rus")) { "Could not initialize Russian OCR" }
   api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
   api.setImage(bitmap)
   val text = api.getUTF8Text().orEmpty()
   val confidence = api.meanConfidence()
   val words = mutableListOf<OcrWord>()
   val iterator = if (text.isNotBlank()) api.getResultIterator() else null
   if (iterator != null) {
    try {
     val level = TessBaseAPI.PageIteratorLevel.RIL_WORD
     iterator.begin()
     do {
      val word = iterator.getUTF8Text(level)?.trim().orEmpty()
      if (word.isNotEmpty()) {
       val box = iterator.getBoundingBox(level)
       if (box != null && box.size == 4) {
        words += OcrWord(word, iterator.confidence(level),
         TextBox(box[0],box[1],box[2],box[3]))
       }
      }
     } while (iterator.next(level))
    } finally { iterator.delete() }
   }
   return OcrResult(text, words, confidence,
    android.os.SystemClock.elapsedRealtime() - start)
  } finally { api.recycle() }
 }
 private fun verifyModel(file: File): Boolean {
  if (!file.isFile || file.length() != 3861738L) return false
  val md = MessageDigest.getInstance("SHA-256")
  file.inputStream().use { input ->
   val buffer = ByteArray(65536)
   while (true) {
    val n = input.read(buffer)
    if (n < 0) break
    md.update(buffer,0,n)
   }
  }
  return md.digest().joinToString("") { "%02x".format(it) } ==
   "e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225"
 }
}
