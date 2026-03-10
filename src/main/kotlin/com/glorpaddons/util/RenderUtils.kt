package com.glorpaddons.util

import net.minecraft.client.gui.DrawContext
import kotlin.math.sqrt

object RenderUtils {

    /**
     * Draws a filled rounded rectangle using DrawContext.fill().
     * @param color ARGB integer (e.g. 0xC8101820.toInt())
     * @param radius Corner radius in pixels
     */
    fun drawRoundedRect(
        context: DrawContext,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Int,
        color: Int
    ) {
        val xi = x.toInt()
        val yi = y.toInt()
        val x2 = (x + width).toInt()
        val y2 = (y + height).toInt()
        val r = radius.coerceAtMost((width / 2).toInt()).coerceAtMost((height / 2).toInt())

        // Center vertical strip (full height, inner width)
        context.fill(xi + r, yi, x2 - r, y2, color)
        // Left strip
        context.fill(xi, yi + r, xi + r, y2 - r, color)
        // Right strip
        context.fill(x2 - r, yi + r, x2, y2 - r, color)

        // Top corners: scanline from top down, dy = r-i (farthest at i=0, closest at i=r-1)
        for (i in 0 until r) {
            val dy = (r - i).toDouble()
            val dx = sqrt(r.toDouble() * r - dy * dy).toInt()
            context.fill(xi + r - dx, yi + i, xi + r, yi + i + 1, color)       // top-left
            context.fill(x2 - r, yi + i, x2 - r + dx, yi + i + 1, color)      // top-right
        }

        // Bottom corners: scanline from bottom up, same dy logic
        for (i in 0 until r) {
            val dy = (r - i).toDouble()
            val dx = sqrt(r.toDouble() * r - dy * dy).toInt()
            context.fill(xi + r - dx, y2 - 1 - i, xi + r, y2 - i, color)      // bottom-left
            context.fill(x2 - r, y2 - 1 - i, x2 - r + dx, y2 - i, color)     // bottom-right
        }
    }
}
