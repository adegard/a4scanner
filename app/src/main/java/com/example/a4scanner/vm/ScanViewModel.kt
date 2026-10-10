package com.example.a4scanner.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a4scanner.data.PageCorners
import com.example.a4scanner.data.PageFilter
import com.example.a4scanner.data.ScanPage
import com.example.a4scanner.image.BitmapLoader
import com.example.a4scanner.image.Filters
import com.example.a4scanner.image.Perspective
import com.example.a4scanner.pdf.PdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val capturesDir = File(app.cacheDir, "captures").apply { mkdirs() }
    private val pagesDir = File(app.filesDir, "pages").apply { mkdirs() }
    private val scansDir = File(app.getExternalFilesDir(null) ?: app.filesDir, "scans").apply { mkdirs() }

    private val _pages = MutableStateFlow<List<ScanPage>>(emptyList())
    val pages: StateFlow<List<ScanPage>> = _pages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun createCaptureFile(): File =
        File(capturesDir, "capture_${System.currentTimeMillis()}.jpg")

    fun addPage(
        sourceFile: File,
        corners: PageCorners,
        filter: PageFilter,
        rotationDegrees: Int
    ) {
        _busy.value = true
        viewModelScope.launch {
            val processed = withContext(Dispatchers.Default) {
                process(sourceFile.absolutePath, corners, filter, rotationDegrees)
            }
            val page = ScanPage(
                sourcePath = sourceFile.absolutePath,
                corners = corners,
                filter = filter,
                rotationDegrees = rotationDegrees,
                processedPath = processed
            )
            _pages.value = _pages.value + page
            _busy.value = false
        }
    }

    fun updatePage(pageId: String, corners: PageCorners, filter: PageFilter, rotationDegrees: Int) {
        val existing = _pages.value.firstOrNull { it.id == pageId } ?: return
        _busy.value = true
        viewModelScope.launch {
            val processed = withContext(Dispatchers.Default) {
                process(existing.sourcePath, corners, filter, rotationDegrees)
            }
            existing.processedPath?.let { File(it).delete() }
            _pages.value = _pages.value.map {
                if (it.id == pageId) it.copy(
                    corners = corners,
                    filter = filter,
                    rotationDegrees = rotationDegrees,
                    processedPath = processed
                ) else it
            }
            _busy.value = false
        }
    }

    fun deletePage(pageId: String) {
        val page = _pages.value.firstOrNull { it.id == pageId } ?: return
        page.processedPath?.let { File(it).delete() }
        File(page.sourcePath).delete()
        _pages.value = _pages.value.filterNot { it.id == pageId }
    }

    fun movePage(from: Int, to: Int) {
        val list = _pages.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _pages.value = list
    }

    fun discardCapture(file: File) {
        file.delete()
    }

    fun exportPdf(onResult: (File) -> Unit) {
        val current = _pages.value
        if (current.isEmpty()) return
        _busy.value = true
        viewModelScope.launch {
            val file = withContext(Dispatchers.Default) {
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val out = File(scansDir, "Scan_$stamp.pdf")
                val bitmaps = ArrayList<Bitmap>(current.size)
                current.forEach { page ->
                    BitmapFactory.decodeFile(page.displayPath())?.let { bitmaps.add(it) }
                }
                try {
                    PdfBuilder.build(bitmaps, out)
                } finally {
                    bitmaps.forEach { it.recycle() }
                }
                out
            }
            _busy.value = false
            onResult(file)
        }
    }

    private fun process(
        sourcePath: String,
        corners: PageCorners,
        filter: PageFilter,
        rotationDegrees: Int
    ): String {
        val source = BitmapLoader.loadOriented(File(sourcePath), rotationDegrees)
        val rectified = Perspective.rectify(source, corners)
        if (rectified !== source) source.recycle()
        val filtered = Filters.apply(rectified, filter)
        if (filtered !== rectified) rectified.recycle()

        val outFile = File(pagesDir, "page_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { stream ->
            filtered.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        }
        filtered.recycle()
        return outFile.absolutePath
    }
}
