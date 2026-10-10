package com.example.a4scanner.data

import java.util.UUID

enum class PageFilter(val label: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    HIGH_CONTRAST("High contrast"),
    BLACK_WHITE("Black & white")
}

data class NormPoint(val x: Float, val y: Float) {
    fun clamped(): NormPoint = NormPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

data class PageCorners(
    val topLeft: NormPoint,
    val topRight: NormPoint,
    val bottomRight: NormPoint,
    val bottomLeft: NormPoint
) {
    fun asList(): List<NormPoint> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun rotateClockwise(): PageCorners {
        fun r(p: NormPoint) = NormPoint(1f - p.y, p.x)
        return PageCorners(
            topLeft = r(bottomLeft),
            topRight = r(topLeft),
            bottomRight = r(topRight),
            bottomLeft = r(bottomRight)
        )
    }

    companion object {
        fun default(inset: Float = 0.04f): PageCorners = PageCorners(
            topLeft = NormPoint(inset, inset),
            topRight = NormPoint(1f - inset, inset),
            bottomRight = NormPoint(1f - inset, 1f - inset),
            bottomLeft = NormPoint(inset, 1f - inset)
        )
    }
}

data class ScanPage(
    val id: String = UUID.randomUUID().toString(),
    val sourcePath: String,
    val corners: PageCorners = PageCorners.default(),
    val filter: PageFilter = PageFilter.ORIGINAL,
    val rotationDegrees: Int = 0,
    val processedPath: String? = null
) {
    fun displayPath(): String = processedPath ?: sourcePath
}
