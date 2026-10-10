package com.example.a4scanner.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.a4scanner.data.PageFilter

object Filters {

    fun apply(src: Bitmap, filter: PageFilter): Bitmap = when (filter) {
        PageFilter.ORIGINAL -> src
        PageFilter.GRAYSCALE -> withColorMatrix(src, ColorMatrix().apply { setSaturation(0f) })
        PageFilter.HIGH_CONTRAST -> withColorMatrix(src, highContrastMatrix())
        PageFilter.BLACK_WHITE -> adaptiveThreshold(src)
    }

    private fun highContrastMatrix(): ColorMatrix {
        val gray = ColorMatrix().apply { setSaturation(0f) }
        val contrast = 1.7f
        val translate = 128f * (1f - contrast)
        val c = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        gray.postConcat(c)
        return gray
    }

    private fun withColorMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun adaptiveThreshold(src: Bitmap, windowFraction: Float = 0.14f, t: Float = 0.15f): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val gray = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = (r * 77 + g * 150 + b * 29) shr 8
        }

        val stride = w + 1
        val integral = LongArray(stride * (h + 1))
        for (y in 0 until h) {
            var rowSum = 0L
            val rowBase = y * w
            val intRow = (y + 1) * stride
            val prevRow = y * stride
            for (x in 0 until w) {
                rowSum += gray[rowBase + x]
                integral[intRow + x + 1] = integral[prevRow + x + 1] + rowSum
            }
        }

        val rad = (minOf(w, h) * windowFraction).toInt().coerceAtLeast(1)
        val out = IntArray(w * h)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        for (y in 0 until h) {
            val y1 = (y - rad).coerceAtLeast(0)
            val y2 = (y + rad).coerceAtMost(h - 1)
            val rowBase = y * w
            val topRow = y1 * stride
            val bottomRow = (y2 + 1) * stride
            for (x in 0 until w) {
                val x1 = (x - rad).coerceAtLeast(0)
                val x2 = (x + rad).coerceAtMost(w - 1)
                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                val sum = integral[bottomRow + x2 + 1] - integral[topRow + x2 + 1] -
                    integral[bottomRow + x1] + integral[topRow + x1]
                val g = gray[rowBase + x]
                out[rowBase + x] = if (g * count <= sum * (1f - t)) black else white
            }
        }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }
}
