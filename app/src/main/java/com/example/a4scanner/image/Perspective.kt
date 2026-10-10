package com.example.a4scanner.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.example.a4scanner.data.PageCorners
import kotlin.math.hypot

object Perspective {

    private const val MAX_DIM = 2480

    fun rectify(src: Bitmap, corners: PageCorners): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val tl = corners.topLeft
        val tr = corners.topRight
        val br = corners.bottomRight
        val bl = corners.bottomLeft

        val tlP = floatArrayOf(tl.x * w, tl.y * h)
        val trP = floatArrayOf(tr.x * w, tr.y * h)
        val brP = floatArrayOf(br.x * w, br.y * h)
        val blP = floatArrayOf(bl.x * w, bl.y * h)

        val srcPts = floatArrayOf(
            tlP[0], tlP[1],
            trP[0], trP[1],
            brP[0], brP[1],
            blP[0], blP[1]
        )

        val topLen = dist(tlP, trP)
        val bottomLen = dist(blP, brP)
        val leftLen = dist(tlP, blP)
        val rightLen = dist(trP, brP)

        var outW = (topLen + bottomLen) / 2f
        var outH = (leftLen + rightLen) / 2f
        outW = outW.coerceAtLeast(1f)
        outH = outH.coerceAtLeast(1f)

        val scale = (MAX_DIM / maxOf(outW, outH)).coerceAtMost(1f)
        val dstW = (outW * scale).toInt().coerceAtLeast(32)
        val dstH = (outH * scale).toInt().coerceAtLeast(32)

        val dst = floatArrayOf(
            0f, 0f,
            dstW.toFloat(), 0f,
            dstW.toFloat(), dstH.toFloat(),
            0f, dstH.toFloat()
        )

        val matrix = Matrix()
        if (!matrix.setPolyToPoly(srcPts, 0, dst, 0, 4)) {
            return src
        }

        val out = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(src, matrix, paint)
        return out
    }

    private fun dist(a: FloatArray, b: FloatArray): Float =
        hypot((a[0] - b[0]).toDouble(), (a[1] - b[1]).toDouble()).toFloat()
}
