package com.sl.fairprice.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Must execute on arm64 Android, not claimed by JVM/desktop tests. */
@RunWith(AndroidJUnit4::class)
class RussianOcrSmokeTest {
 @Test fun syntheticCyrillicTagRecognizesOffline() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val bitmap=Bitmap.createBitmap(1800,400,Bitmap.Config.ARGB_8888)
  val canvas=Canvas(bitmap)
  canvas.drawColor(Color.WHITE)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
   color=Color.BLACK; textSize=90f; isFakeBoldText=true
  }
  canvas.drawText("Молоко 900 мл",60f,160f,paint)
  canvas.drawText("129,99 руб.",60f,310f,paint)
  try {
   val result=TesseractOfflineOcr(context).recognize(bitmap)
   assertTrue("Cyrillic not recognized: "+result.text,
    result.text.any { it in 'А'..'я' })
   assertTrue("Quantity absent: "+result.text,result.text.contains("900"))
   assertTrue("Price absent: "+result.text,result.text.contains("129"))
   assertTrue("No word boxes",result.words.isNotEmpty())
   assertTrue(result.words.all {
    it.box.right>it.box.left && it.box.bottom>it.box.top
   })
   assertTrue(result.meanConfidence in 0..100)
   assertTrue(result.elapsedMs>=0L)
  } finally { bitmap.recycle() }
 }
 @Test fun noInternetPermission() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  @Suppress("DEPRECATION")
  val p=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_PERMISSIONS)
  assertFalse(p.requestedPermissions.orEmpty().contains("android.permission.INTERNET"))
 }
}
