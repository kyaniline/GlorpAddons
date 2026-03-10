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

    /**
     * Finds "Area: Dwarven Mines" (or Glacite Tunnels) in the tab list.
     */
    private fun detectArea(lines: List<String>): String? {
        val areaRegex = Regex("""^Area:\s*(.+)$""")
        for (line in lines) {
            val match = areaRegex.find(line.trim()) ?: continue
            val area = match.groupValues[1].trim()
            if (ALLOWED_AREAS.any { it.equals(area, ignoreCase = true) }) return area
        }
        return null
    }

    // ── Commission parsing ───────────────────────────────────────────────────

    /**
     * The Hypixel tab list commission widget format:
     *
     *   Commissions:
     *   Royal Mines Mithril: 0%
     *   Lucky Raffle: 0%
     *   Goblin Raid Slayer: 0%
     *   Treasure Hoarder Puncher: 30%
     *   Info          ← section ends here
     */
    private fun parseCommissions(lines: List<String>): List<Commission> {
        val result = mutableListOf<Commission>()
        var inSection = false

        val percentRegex = Regex("""^(.+?):\s*(\d+)%$""")
        val doneRegex    = Regex("""^(.+?):\s*DONE$""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.equals("Commissions:", ignoreCase = true)) {
                inSection = true
                continue
            }

            if (!inSection) continue

            // Any non-commission header ends the section
            if (trimmed.isEmpty() || trimmed.equals("Info", ignoreCase = true)) break

            // "Name: XX%"
            percentRegex.find(trimmed)?.let {
                val name    = it.groupValues[1].trim()
                val percent = it.groupValues[2].toIntOrNull() ?: 0
                result.add(Commission(name, percent, 100))
                return@let
            } ?: doneRegex.find(trimmed)?.let {
                // "Name: DONE"
                result.add(Commission(it.groupValues[1].trim(), 100, 100))
            }
        }

        return result
    }

    private fun stripFormatting(text: String): String =
        text.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")
}
