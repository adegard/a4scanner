package com.example.a4scanner.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.a4scanner.data.NormPoint
import com.example.a4scanner.data.PageCorners
import com.example.a4scanner.data.PageFilter
import com.example.a4scanner.image.BitmapLoader
import com.example.a4scanner.image.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

private data class CropGeometry(
    val offX: Float,
    val offY: Float,
    val dispW: Float,
    val dispH: Float
) {
    fun toPx(p: Offset): Offset = Offset(offX + p.x * dispW, offY + p.y * dispH)
}

@Composable
fun CropScreen(
    sourceFile: File,
    initialCorners: PageCorners,
    initialFilter: PageFilter,
    initialRotation: Int,
    isNewCapture: Boolean,
    onApply: (PageCorners, PageFilter, Int) -> Unit,
    onCancel: () -> Unit
) {
    val density = LocalDensity.current
    var rotation by remember { mutableStateOf(initialRotation) }
    var filter by remember { mutableStateOf(initialFilter) }
    var corners by remember {
        mutableStateOf(initialCorners.asList().map { Offset(it.x, it.y) })
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(sourceFile, rotation) {
        loading = true
        previewBitmap = withContext(Dispatchers.Default) {
            BitmapLoader.loadOriented(sourceFile, rotation, maxDim = 1200)
        }
        loading = false
    }

    val geom = remember(containerSize, previewBitmap) {
        val bmp = previewBitmap
        if (bmp == null || containerSize.width == 0 || containerSize.height == 0) {
            CropGeometry(0f, 0f, 1f, 1f)
        } else {
            val scale = min(
                containerSize.width.toFloat() / bmp.width,
                containerSize.height.toFloat() / bmp.height
            )
            val dispW = bmp.width * scale
            val dispH = bmp.height * scale
            CropGeometry(
                offX = (containerSize.width - dispW) / 2f,
                offY = (containerSize.height - dispH) / 2f,
                dispW = dispW,
                dispH = dispH
            )
        }
    }

    val touchRadius = with(density) { 32.dp.toPx() }

    Surface(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(geom) {
                        var active = -1
                        detectDragGestures(
                            onDragStart = { pos ->
                                active = corners.indices
                                    .minByOrNull { (geom.toPx(corners[it]) - pos).getDistance() }
                                    ?.takeIf {
                                        (geom.toPx(corners[it]) - pos).getDistance() < touchRadius
                                    } ?: -1
                            },
                            onDragEnd = { active = -1 },
                            onDragCancel = { active = -1 },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val idx = active
                                if (idx >= 0) {
                                    val moved = Offset(
                                        corners[idx].x + dragAmount.x / geom.dispW,
                                        corners[idx].y + dragAmount.y / geom.dispH
                                    )
                                    val clamped = Offset(
                                        moved.x.coerceIn(0f, 1f),
                                        moved.y.coerceIn(0f, 1f)
                                    )
                                    corners = corners.toMutableList().also { it[idx] = clamped }
                                }
                            }
                        )
                    }
            ) {
                val bmp = previewBitmap
                if (bmp != null && !loading) {
                    CropCanvas(
                        image = bmp.asImageBitmap(),
                        geom = geom,
                        corners = corners
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PageFilter.values().forEach { f ->
                    FilterChip(
                        selected = f == filter,
                        onClick = { filter = f },
                        label = { Text(f.label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Text(if (isNewCapture) "Retake" else "Cancel")
                }
                OutlinedButton(
                    onClick = {
                        rotation = (rotation + 90) % 360
                        corners = listOf(
                            Offset(1f - corners[3].y, corners[3].x),
                            Offset(1f - corners[0].y, corners[0].x),
                            Offset(1f - corners[1].y, corners[1].x),
                            Offset(1f - corners[2].y, corners[2].x)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
                    Text("Rotate")
                }
                Button(
                    onClick = {
                        val pageCorners = PageCorners(
                            topLeft = NormPoint(corners[0].x, corners[0].y),
                            topRight = NormPoint(corners[1].x, corners[1].y),
                            bottomRight = NormPoint(corners[2].x, corners[2].y),
                            bottomLeft = NormPoint(corners[3].x, corners[3].y)
                        )
                        onApply(pageCorners, filter, rotation)
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun CropCanvas(
    image: androidx.compose.ui.graphics.ImageBitmap,
    geom: CropGeometry,
    corners: List<Offset>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dstOffset = IntOffset(geom.offX.toInt(), geom.offY.toInt())
        val dstSize = IntSize(geom.dispW.toInt(), geom.dispH.toInt())

        drawImage(
            image = image,
            dstOffset = dstOffset,
            dstSize = dstSize,
            filterQuality = FilterQuality.Medium
        )

        val px = corners.map { geom.toPx(it) }
        val quad = Path().apply {
            moveTo(px[0].x, px[0].y)
            lineTo(px[1].x, px[1].y)
            lineTo(px[2].x, px[2].y)
            lineTo(px[3].x, px[3].y)
            close()
        }

        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(geom.offX, geom.offY),
            size = androidx.compose.ui.geometry.Size(geom.dispW, geom.dispH)
        )
        clipPath(quad) {
            drawImage(
                image = image,
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = FilterQuality.Medium
            )
        }
        drawPath(
            path = quad,
            color = Color(0xFF1565C0),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        px.forEach { handle ->
            drawCircle(color = Color.White, radius = 16f, center = handle)
            drawCircle(
                color = Color(0xFF1565C0),
                radius = 16f,
                center = handle,
                style = Stroke(width = 4f)
            )
        }
    }
}
