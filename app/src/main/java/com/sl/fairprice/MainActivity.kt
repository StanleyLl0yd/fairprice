package com.sl.fairprice

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sl.fairprice.camera.OcrImageLoader
import com.sl.fairprice.ocr.OcrSelfTest
import com.sl.fairprice.ocr.TesseractOfflineOcr
import com.sl.fairprice.parser.PriceKind
import com.sl.fairprice.parser.PriceLabelParser
import com.sl.fairprice.ui.CompareScreen
import com.sl.fairprice.ui.OfferDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var offers by remember { mutableStateOf(listOf(OfferDraft(), OfferDraft())) }
                var photoTarget by remember { mutableIntStateOf(0) }
                var busy by remember { mutableStateOf(false) }
                var failure by remember { mutableStateOf<String?>(null) }
                var selfTestReport by remember { mutableStateOf<String?>(null) }
                var reportCopied by remember { mutableStateOf(false) }

                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null && !busy) {
                        val target = photoTarget
                        scope.launch {
                            busy = true
                            failure = null
                            try {
                                val recognized = withContext(Dispatchers.IO) {
                                    val bitmap = OcrImageLoader.decode(this@MainActivity, uri)
                                    try {
                                        TesseractOfflineOcr(this@MainActivity).recognize(bitmap)
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                                val parsed = PriceLabelParser.parse(recognized.text)
                                // A reference price or card-only price must never silently
                                // be used as the normal package price.
                                val regular = parsed.prices.filter { it.kind == PriceKind.REGULAR }
                                    .singleOrNull()
                                val quantity = parsed.quantities.singleOrNull()
                                val details = getString(
                                    R.string.ocr_candidate_counts,
                                    parsed.prices.size,
                                    parsed.quantities.size,
                                    recognized.meanConfidence
                                )
                                offers = offers.mapIndexed { index, offer ->
                                    if (index != target) offer else offer.copy(
                                        price = regular?.amountRub?.toPlainString() ?: "",
                                        quantity = quantity?.amount?.toPlainString() ?: "",
                                        measure = quantity?.measure ?: offer.measure,
                                        confirmed = false,
                                        ocrText = recognized.text.take(1500),
                                        ocrDetails = details
                                    )
                                }
                            } catch (e: Exception) {
                                failure = e.message ?: e.javaClass.simpleName
                            } finally {
                                busy = false
                            }
                        }
                    }
                }

                Scaffold { inner ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CompareScreen(
                            offers = offers,
                            busy = busy,
                            onEdit = { index, updated ->
                                offers = offers.mapIndexed { i, original ->
                                    if (i == index) updated.copy(confirmed = false) else original
                                }
                            },
                            onConfirm = { index, confirmed ->
                                offers = offers.mapIndexed { i, original ->
                                    if (i == index) original.copy(confirmed = confirmed) else original
                                }
                            },
                            onPhoto = { index ->
                                photoTarget = index
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        if (busy) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.processing))
                        }
                        failure?.let {
                            Text(stringResource(R.string.error_prefix, it),
                                color = MaterialTheme.colorScheme.error)
                        }
                        HorizontalDivider()
                        Text(stringResource(R.string.diagnostics_title),
                            style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.diagnostics_description))
                        Button(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                failure = null
                                selfTestReport = null
                                reportCopied = false
                                try {
                                    val test = withContext(Dispatchers.Default) {
                                        OcrSelfTest.run(this@MainActivity)
                                    }
                                    selfTestReport = test.diagnostic
                                } catch (e: Exception) {
                                    failure = e.message ?: e.javaClass.simpleName
                                } finally {
                                    busy = false
                                }
                            }
                        }) { Text(stringResource(R.string.run_self_test)) }
                        selfTestReport?.let { report ->
                            Text(report)
                            Button(onClick = {
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("FairPrice OCR diagnostics", report))
                                reportCopied = true
                            }) { Text(stringResource(R.string.copy_report)) }
                            if (reportCopied) Text(stringResource(R.string.report_copied))
                        }
                    }
                }
            }
        }
    }
}
