package com.sl.fairprice.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.IOException

/**
 * Off-main-thread Photo Picker decode with a bounded bitmap and EXIF geometry.
 * Caller owns the result and must recycle it. No network or extra permission.
 */
object OcrImageLoader {
    fun decode(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap {
        require(maxDimension > 0)
        val resolver = context.contentResolver
        val orientation = resolver.openInputStream(uri)?.use { input ->
            try {
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: IOException) {
                ExifInterface.ORIENTATION_NORMAL
            }
        } ?: throw IOException("Unable to open image metadata")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IOException("Unable to open image")
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        require(longest > 0) { "Invalid image dimensions" }
        var sample = 1
        while (longest.toLong() / sample > maxDimension) sample *= 2
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val original = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IOException("Unable to decode image")

        try {
            val fixed = orient(original, orientation)
            if (fixed !== original) original.recycle()
            return fixed
        } catch (e: Exception) {
            original.recycle()
            throw e
        }
    }

    internal fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
