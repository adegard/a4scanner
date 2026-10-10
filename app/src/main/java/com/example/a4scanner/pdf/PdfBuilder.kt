package com.example.a4scanner.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfBuilder {

    const val A4_WIDTH = 595
    const val A4_HEIGHT = 842

    fun build(pages: List<Bitmap>, output: File): File {
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, bitmap ->
                val info = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, index + 1).create()
                val page = document.startPage(info)
                val canvas: Canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                drawFitted(canvas, bitmap)
                document.finishPage(page)
            }
            output.parentFile?.mkdirs()
            FileOutputStream(output).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return output
    }

    private fun drawFitted(canvas: Canvas, bitmap: Bitmap) {
        val scale = minOf(
            A4_WIDTH.toFloat() / bitmap.width,
            A4_HEIGHT.toFloat() / bitmap.height
        )
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = (A4_WIDTH - drawW) / 2f
        val top = (A4_HEIGHT - drawH) / 2f
        val dst = RectF(left, top, left + drawW, top + drawH)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(bitmap, null, dst, paint)
    }
}
