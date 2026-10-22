package com.sl.fairprice.ocr

import android.graphics.Bitmap
import com.sl.fairprice.model.OcrResult

interface OfflineOcr {
 // Blocking operation; dispatch off main thread.
 fun recognize(bitmap:Bitmap):OcrResult
}
