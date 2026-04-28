package com.glorpaddons.commissions

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.PlayerListEntry

object CommissionTracker {

    private val ALLOWED_AREAS = setOf("Dwarven Mines", "Glacite Tunnels", "Glacite Mineshaft")

    var currentArea: String? = null
        private set

    var commissions: List<Commission> = emptyList()
        private set

    val isInAllowedArea: Boolean
        get() = currentArea != null

    fun tick() {
        val client = MinecraftClient.getInstance()
        if (client.world == null) {
            reset()
            return
        }

        val lines = getTabListLines(client)
        currentArea = detectArea(lines)
        commissions = parseCommissions(lines)
    }

    private fun reset() {
        currentArea = null
        commissions = emptyList()
    }

    // ── Tab list ─────────────────────────────────────────────────────────────

    fun getTabListLines(client: MinecraftClient): List<String> {
        val networkHandler = client.networkHandler ?: return emptyList()
        return networkHandler.playerList
            .sortedWith(
                compareBy<PlayerListEntry> { it.scoreboardTeam?.name ?: "" }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.profile.name }
            )
            .mapNotNull { entry ->
                val text = entry.displayName?.string ?: entry.profile.name
                stripFormatting(text).trim().takeIf { it.isNotEmpty() }
            }
    }

    // ── Area detection ───────────────────────────────────────────────────────

    private fun detectArea(lines: List<String>): String? {
        val areaRegex = Regex("""Area:\s*(.+)""")
        for (line in lines) {
            val match = areaRegex.find(line.trim()) ?: continue
            val area = match.groupValues[1].trim()
            if (ALLOWED_AREAS.any { it.equals(area, ignoreCase = true) }) return area
        }
        return null
    }

    // ── Commission parsing ───────────────────────────────────────────────────

    private val percentRegex = Regex("""(.+?):\s*(\d+(?:\.\d+)?)%""")
    private val doneRegex    = Regex("""(.+?):\s*DONE""", RegexOption.IGNORE_CASE)

    private fun parseCommissions(lines: List<String>): List<Commission> {
        val result = mutableListOf<Commission>()
        var inSection = false
        var linesSinceHeader = 0

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("Commissions", ignoreCase = true)) {
                inSection = true
                linesSinceHeader = 0
                continue
            }

            if (!inSection) continue

            linesSinceHeader++

            // Safety: commissions section is at most ~6 lines after header
            if (linesSinceHeader > 10) break

            // Skip blank spacer lines
            if (trimmed.isEmpty()) continue

            // Known section terminators
            if (trimmed.equals("Info", ignoreCase = true)) break

            // "Name: XX%" or "Name: XX.X%"
            percentRegex.find(trimmed)?.let {
                val name    = it.groupValues[1].trim()
                val percent = it.groupValues[2].toFloatOrNull()?.toInt() ?: 0
                result.add(Commission(name, percent, 100))
            } ?: doneRegex.find(trimmed)?.let {
                result.add(Commission(it.groupValues[1].trim(), 100, 100))
            }
        }

        return result
    }

    /** Strip all § formatting codes including hex color §x sequences. */
    private fun stripFormatting(text: String): String =
        text.replace(Regex("§."), "")
}
