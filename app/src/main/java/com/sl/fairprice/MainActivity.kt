package com.sl.fairprice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sl.fairprice.camera.PriceCameraActivity
import com.sl.fairprice.camera.OcrImageLoader
import java.io.File
import com.sl.fairprice.history.ComparisonHistory
import com.sl.fairprice.history.SavedComparison
import com.sl.fairprice.pricing.PriceEntry
import com.sl.fairprice.ui.HistorySection
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
                var historyRows by remember { mutableStateOf<List<SavedComparison>>(emptyList()) }
                val historyDb = remember { ComparisonHistory(applicationContext) }
                LaunchedEffect(Unit) {
                    try {
                        historyRows = withContext(Dispatchers.IO) { historyDb.list() }
                    } catch (e: Exception) {
                        failure = e.message ?: e.javaClass.simpleName
                    }
                }

                fun processOfferImage(uri: Uri, target: Int, ephemeral: File? = null) {
                    if (busy) {
                        ephemeral?.delete()
                        return
                    }
                    scope.launch {
                        busy = true
                        failure = null
                        try {
                            val recognized = withContext(Dispatchers.IO) {
                                val bitmap = OcrImageLoader.decode(this@MainActivity, uri)
                                try { TesseractOfflineOcr(this@MainActivity).recognize(bitmap) }
                                finally { bitmap.recycle() }
                            }
                            val parsed = PriceLabelParser.parse(recognized.text)
                            // Reference unit and loyalty-only prices are never silently trusted.
                            val regular = parsed.prices.filter {
                                it.kind == PriceKind.REGULAR
                            }.singleOrNull()
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
                            ephemeral?.delete() // private capture cache, never retained
                            busy = false
                        }
                    }
                }

                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) processOfferImage(uri, photoTarget)
                }

                val cameraResult = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val path = result.data?.getStringExtra(
                            PriceCameraActivity.EXTRA_LOCAL_IMAGE_PATH
                        )
                        val directory = File(cacheDir, PriceCameraActivity.CAPTURE_DIR).canonicalFile
                        val photo = path?.let { File(it).canonicalFile }
                        if (photo != null && photo.isFile && photo.parentFile == directory &&
                            photo.extension.equals("jpg", ignoreCase = true)) {
                            processOfferImage(Uri.fromFile(photo), photoTarget, photo)
                        } else {
                            failure = getString(R.string.camera_invalid_result)
                        }
                    }
                }
                val permission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        cameraResult.launch(Intent(this@MainActivity, PriceCameraActivity::class.java))
                    } else {
                        failure = getString(R.string.camera_permission_denied)
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
                            },
                            onCamera = { index ->
                                if (!busy) {
                                    photoTarget = index
                                    if (ContextCompat.checkSelfPermission(
                                            this@MainActivity, Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED) {
                                        cameraResult.launch(Intent(this@MainActivity, PriceCameraActivity::class.java))
                                    } else {
                                        permission.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            }
                        )
                        val offerA = PriceEntry.offer(offers[0].price, offers[0].quantity, offers[0].measure)
                        val offerB = PriceEntry.offer(offers[1].price, offers[1].quantity, offers[1].measure)
                        HistorySection(
                            rows = historyRows,
                            canSave = offerA != null && offerB != null &&
                                offerA.measure == offerB.measure && offers.all { it.confirmed },
                            busy = busy,
                            onSave = {
                                val a = offerA
                                val b = offerB
                                if (a != null && b != null && offers.all { it.confirmed } &&
                                    a.measure == b.measure) {
                                    scope.launch {
                                        try {
                                            historyRows = withContext(Dispatchers.IO) {
                                                historyDb.save(a, b)
                                                historyDb.list()
                                            }
                                        } catch (e: Exception) {
                                            failure = e.message ?: e.javaClass.simpleName
                                        }
                                    }
                                }
                            },
                            onRestore = { row ->
                                offers = listOf(row.first, row.second).map { product ->
                                    OfferDraft(
                                        price = product.priceRub.toPlainString(),
                                        quantity = product.quantity.toPlainString(),
                                        measure = product.measure,
                                        confirmed = false
                                    )
                                }
                            },
                            onDelete = { id ->
                                scope.launch {
                                    try {
                                        historyRows = withContext(Dispatchers.IO) {
                                            historyDb.delete(id)
                                            historyDb.list()
                                        }
                                    } catch (e: Exception) {
                                        failure = e.message ?: e.javaClass.simpleName
                                    }
                                }
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
