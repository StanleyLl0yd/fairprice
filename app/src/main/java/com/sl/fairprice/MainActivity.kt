package com.sl.fairprice

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sl.fairprice.model.OcrResult
import com.sl.fairprice.ocr.TesseractOfflineOcr
import com.sl.fairprice.ocr.OcrSelfTest
import com.sl.fairprice.parser.Measure
import com.sl.fairprice.parser.PriceKind
import com.sl.fairprice.parser.PriceLabelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   MaterialTheme {
    val scope=rememberCoroutineScope()
    var result by remember { mutableStateOf<OcrResult?>(null) }
    var running by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var selfTestReport by remember { mutableStateOf<String?>(null) }
    var reportCopied by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
     if (uri != null && !running) scope.launch {
      running=true; result=null; failure=null; selfTestReport=null; reportCopied=false
      try {
       result=withContext(Dispatchers.IO) {
        val bitmap=com.sl.fairprice.camera.OcrImageLoader.decode(this@MainActivity,uri)
        try { TesseractOfflineOcr(this@MainActivity).recognize(bitmap) }
        finally { bitmap.recycle() }
       }
      } catch (e:Exception) { failure=e.message ?: e.javaClass.simpleName }
      finally { running=false }
     }
    }
    Scaffold { padding ->
     Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)
      .verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text(stringResource(R.string.app_name),style=MaterialTheme.typography.headlineMedium)
      Text(stringResource(R.string.probe_subtitle))
      Button(enabled=!running, onClick={
       scope.launch {
        running=true; result=null; failure=null; selfTestReport=null; reportCopied=false
        try {
         val probe=withContext(Dispatchers.Default) {
          OcrSelfTest.run(this@MainActivity)
         }
         result=probe.recognized
         selfTestReport=probe.diagnostic
        } catch(e:Exception) { failure=e.message ?: e.javaClass.simpleName }
        finally { running=false }
       }
      }) { Text(stringResource(R.string.run_self_test)) }
      Button(enabled=!running, onClick={
       picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      }) { Text(stringResource(R.string.pick_photo)) }
      if (running) {
       CircularProgressIndicator()
       Text(stringResource(R.string.processing))
      }
      failure?.let { Text(stringResource(R.string.error_prefix,it),
       color=MaterialTheme.colorScheme.error) }
      selfTestReport?.let { report ->
       HorizontalDivider()
       Text(stringResource(R.string.self_test_result),style=MaterialTheme.typography.titleMedium)
       Text(report)
       Button(onClick={
        val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FairPrice synthetic OCR report",report))
        reportCopied=true
       }) { Text(stringResource(R.string.copy_report)) }
       if(reportCopied) Text(stringResource(R.string.report_copied))
      }
      result?.let { ocr ->
       Text(stringResource(R.string.ocr_confidence,ocr.meanConfidence))
       Text(stringResource(R.string.ocr_elapsed,ocr.elapsedMs))
       val parsed=PriceLabelParser.parse(ocr.text)
       HorizontalDivider()
       Text(stringResource(R.string.price_candidates), style=MaterialTheme.typography.titleMedium)
       if (parsed.prices.isEmpty()) Text(stringResource(R.string.no_prices))
       parsed.prices.forEach {
        val label=when(it.kind) {
         PriceKind.REGULAR -> stringResource(R.string.source_normal)
         PriceKind.CARD -> stringResource(R.string.source_card)
         PriceKind.REFERENCE -> stringResource(R.string.source_reference)
        }
        Text(it.amountRub.toPlainString()+" ₽ | "+label)
       }
       Text(stringResource(R.string.quantity_candidates),style=MaterialTheme.typography.titleMedium)
       if (parsed.quantities.isEmpty()) Text(stringResource(R.string.no_quantity))
       parsed.quantities.forEach {
        val unit=when(it.measure) {
         Measure.GRAM -> "г"
         Measure.MILLILITRE -> "мл"
         Measure.PIECE -> "шт"
        }
        Text(it.amount.toPlainString()+" "+unit)
       }
       Text(stringResource(R.string.needs_review),color=MaterialTheme.colorScheme.error)
       HorizontalDivider()
       Text(stringResource(R.string.ocr_result),style=MaterialTheme.typography.titleMedium)
       Text(ocr.text.ifBlank { stringResource(R.string.empty_text) })
       Text(stringResource(R.string.word_bounds),style=MaterialTheme.typography.titleMedium)
       ocr.words.forEach {
        Text(it.text+" ["+it.box.left+","+it.box.top+","+it.box.right+","+it.box.bottom+"]: "+it.confidence)
       }
      }
     }
    }
   }
  }
 }
}
