package com.sl.fairprice.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrImageLoaderTest {
    @Test fun portraitRotationChangesDimensions() {
        val original = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.WHITE)
        try {
            val rotated = OcrImageLoader.orient(original, ExifInterface.ORIENTATION_ROTATE_90)
            try {
                assertEquals(8, rotated.width)
                assertEquals(12, rotated.height)
                assertFalse(rotated.isRecycled)
            } finally {
                if (rotated !== original) rotated.recycle()
            }
        } finally { original.recycle() }
    }

    @Test fun normalOrientationReturnsSameBitmap() {
        val original = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
        try {
            assertSame(original, OcrImageLoader.orient(original, ExifInterface.ORIENTATION_NORMAL))
        } finally { original.recycle() }
    }

    @Test fun flippedImageChangesPixelOrder() {
        val original = Bitmap.createBitmap(16, 8, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.RED)
        original.setPixel(0, 0, Color.BLUE)
        try {
            val flipped = OcrImageLoader.orient(original, ExifInterface.ORIENTATION_FLIP_HORIZONTAL)
            try {
                assertEquals(Color.BLUE, flipped.getPixel(flipped.width - 1, 0))
            } finally { if (flipped !== original) flipped.recycle() }
        } finally { original.recycle() }
    }
}
