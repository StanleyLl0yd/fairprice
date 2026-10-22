package com.sl.fairprice.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sl.fairprice.R
import java.io.File

/**
 * A short-lived local CameraX capture surface. No MediaStore/gallery write,
 * location, broad storage access, network, or silent OCR approval.
 *
 * Launch only after a foreground CAMERA runtime permission grant.
 * The receiving screen MUST remove the temporary photo after OCR finishes.
 */
class PriceCameraActivity : ComponentActivity() {
    companion object {
        const val EXTRA_LOCAL_IMAGE_PATH = "com.sl.fairprice.LOCAL_IMAGE_PATH"
        const val CAPTURE_DIR = "ocr-camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            MaterialTheme {
                CaptureScreen(
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSaved = { imageFile ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_LOCAL_IMAGE_PATH, imageFile.absolutePath)
                        )
                        finish()
                    }
                )
            }
        }
    }

    @Composable
    private fun CaptureScreen(
        onCancel: () -> Unit,
        onSaved: (File) -> Unit
    ) {
        val previewView = remember {
            PreviewView(this@PriceCameraActivity).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
        var capture by remember { mutableStateOf<ImageCapture?>(null) }
        var failure by remember { mutableStateOf<String?>(null) }
        var taking by remember { mutableStateOf(false) }

        DisposableEffect(previewView) {
            val future = ProcessCameraProvider.getInstance(this@PriceCameraActivity)
            var disposed = false
            var activeProvider: ProcessCameraProvider? = null
            future.addListener({
                if (!disposed) {
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build()
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        preview.surfaceProvider = previewView.surfaceProvider
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            this@PriceCameraActivity,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        activeProvider = provider
                        capture = imageCapture
                    } catch (e: Exception) {
                        failure = e.message ?: e.javaClass.simpleName
                    }
                }
            }, ContextCompat.getMainExecutor(this@PriceCameraActivity))
            onDispose {
                disposed = true
                capture = null
                activeProvider?.unbindAll()
            }
        }

        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.camera_title), style = MaterialTheme.typography.titleLarge)
            AndroidView(
                factory = { previewView },
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("camera-preview")
            )
            failure?.let {
                Text(stringResource(R.string.camera_failure, it),
                    color = MaterialTheme.colorScheme.error)
            }
            if (capture == null && failure == null) {
                Text(stringResource(R.string.camera_initializing))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, enabled = !taking) {
                    Text(stringResource(R.string.camera_cancel))
                }
                Button(
                    modifier = Modifier.testTag("camera-shutter"),
                    enabled = capture != null && !taking,
                    onClick = {
                        val ready = capture ?: return@Button
                        taking = true
                        failure = null
                        val folder = File(cacheDir, CAPTURE_DIR)
                        if (!folder.exists() && !folder.mkdirs()) {
                            taking = false
                            failure = "Cannot create temporary camera directory"
                            return@Button
                        }
                        val file = try {
                            File.createTempFile("price_", ".jpg", folder)
                        } catch (e: Exception) {
                            taking = false
                            failure = e.message ?: e.javaClass.simpleName
                            return@Button
                        }
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        try {
                            ready.takePicture(
                                options,
                                ContextCompat.getMainExecutor(this@PriceCameraActivity),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(
                                        output: ImageCapture.OutputFileResults
                                    ) {
                                        taking = false
                                        onSaved(file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        file.delete()
                                        taking = false
                                        failure = exception.message ?: exception.javaClass.simpleName
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            file.delete()
                            taking = false
                            failure = e.message ?: e.javaClass.simpleName
                        }
                    }
                ) {
                    Text(stringResource(R.string.camera_capture))
                }
            }
        }
    }
}
