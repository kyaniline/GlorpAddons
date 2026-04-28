package com.glorpaddons.itemrarity

import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack

object ItemRarityBg {

    // Colors match Skyblocker's ChatFormatting values. Alpha 0x80 = 50%, matching
    // Skyblocker's default itemBackgroundOpacity of 0.5f — this is the BORDER opacity;
    // the center fades to ~6% via the gradient.
    enum class Rarity(val color: Int) {
        ADMIN        (0x80FF0000.toInt()),  // DARK_RED
        VERY_SPECIAL (0x80FF5555.toInt()),  // RED
        SPECIAL      (0x80FF5555.toInt()),  // RED
        DIVINE       (0x8055FFFF.toInt()),  // AQUA      ← ChatFormatting.AQUA
        MYTHIC       (0x80FF55FF.toInt()),  // LIGHT_PURPLE ← ChatFormatting.LIGHT_PURPLE
        LEGENDARY    (0x80FFAA00.toInt()),  // GOLD
        EPIC         (0x80AA00AA.toInt()),  // DARK_PURPLE
        RARE         (0x805555FF.toInt()),  // BLUE
        UNCOMMON     (0x8055FF55.toInt()),  // GREEN
        COMMON       (0x80AAAAAA.toInt()),  // WHITE/gray
        SUPREME      (0x8055FFFF.toInt()),  // AQUA
        ULTIMATE     (0x80AA0000.toInt()),  // DARK_RED
    }

    // Order matters: more-specific strings before any prefix of themselves
    private val PATTERNS = listOf(
        "ADMIN"        to Rarity.ADMIN,
        "VERY SPECIAL" to Rarity.VERY_SPECIAL,
        "SPECIAL"      to Rarity.SPECIAL,
        "DIVINE"       to Rarity.DIVINE,
        "MYTHIC"       to Rarity.MYTHIC,
        "LEGENDARY"    to Rarity.LEGENDARY,
        "EPIC"         to Rarity.EPIC,
        "RARE"         to Rarity.RARE,
        "UNCOMMON"     to Rarity.UNCOMMON,
        "COMMON"       to Rarity.COMMON,
        "SUPREME"      to Rarity.SUPREME,
        "ULTIMATE"     to Rarity.ULTIMATE,
    )

    /** Returns the ARGB color for this stack's rarity, or null if none found. */
    fun getRarityColor(stack: ItemStack): Int? {
        val lore = stack.get(DataComponentTypes.LORE) ?: return null
        for (line in lore.lines().asReversed()) {
            val text = stripFormatting(line.getString())
            if (text.isEmpty()) continue
            // Use indexOf + word-boundary check so recombobulated items (whose rarity
            // line starts with an obfuscated §k placeholder letter like "a LEGENDARY…")
            // are still matched correctly.
            val rarity = PATTERNS.firstOrNull { (prefix, _) ->
                val idx = text.indexOf(prefix)
                idx >= 0 && (idx == 0 || !text[idx - 1].isLetter())
            }?.second
            if (rarity != null) return rarity.color
        }
        return null
    }

    /**
     * Draws a rarity background behind a 16×16 item slot, matching Skyblocker's
     * item_background_square sprite exactly.
     *
     * The sprite encodes five palette alpha levels (189, 133, 89, 77, 71) arranged
     * as concentric 1-pixel rings.  Each pixel is painted exactly once (no overdraw
     * / alpha compounding).  Scaled by the color's own alpha component so overall
     * opacity can be controlled via the color value.
     */
    fun drawBackground(context: DrawContext, x: Int, y: Int, color: Int) {
        val baseAlpha = (color ushr 24) and 0xFF
        val rgb       = color and 0x00FFFFFF

        // Sprite palette alphas × (baseAlpha / 255) — matches Skyblocker at 0x80 (0.5)
        fun c(pa: Int) = ((pa * baseAlpha / 255) shl 24) or rgb
        val a0 = c(189)  // outer ring  → ~94 at 0.5
        val a1 = c(133)  //             → ~66
        val a2 = c( 89)  //             → ~44
        val a3 = c( 77)  //             → ~38
        val a4 = c( 71)  // center      → ~36

        // Ring 0 – outermost 1-pixel border
        context.fill(x,     y,      x + 16, y +  1,  a0)  // top
        context.fill(x,     y + 15, x + 16, y + 16,  a0)  // bottom
        context.fill(x,     y +  1, x +  1, y + 15,  a0)  // left
        context.fill(x + 15, y + 1, x + 16, y + 15,  a0)  // right

        // Ring 1
        context.fill(x +  1, y +  1, x + 15, y +  2,  a1)
        context.fill(x +  1, y + 14, x + 15, y + 15,  a1)
        context.fill(x +  1, y +  2, x +  2, y + 14,  a1)
        context.fill(x + 14, y +  2, x + 15, y + 14,  a1)

        // Ring 2
        context.fill(x +  2, y +  2, x + 14, y +  3,  a2)
        context.fill(x +  2, y + 13, x + 14, y + 14,  a2)
        context.fill(x +  2, y +  3, x +  3, y + 13,  a2)
        context.fill(x + 13, y +  3, x + 14, y + 13,  a2)

        // Ring 3
        context.fill(x +  3, y +  3, x + 13, y +  4,  a3)
        context.fill(x +  3, y + 12, x + 13, y + 13,  a3)
        context.fill(x +  3, y +  4, x +  4, y + 12,  a3)
        context.fill(x + 12, y +  4, x + 13, y + 12,  a3)

        // Center 8×8
        context.fill(x + 4, y + 4, x + 12, y + 12, a4)
    }

    private fun stripFormatting(s: String): String = s.replace(Regex("§."), "").trim()
}
