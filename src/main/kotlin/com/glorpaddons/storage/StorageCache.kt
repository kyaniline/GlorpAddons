package com.glorpaddons.storage

import com.glorpaddons.GlorpAddons
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.screen.slot.Slot
import java.io.File

object StorageCache {

    var pages: List<StoragePage> = emptyList()
        private set

    /** Cached items per page, keyed by StoragePageSlot.index. */
    private val pageItems: MutableMap<Int, List<ItemStack>> = mutableMapOf()

    /** Currently open page, or null when on overview / outside storage. */
    var currentPage: StoragePageSlot? = null
        private set

    var wasInOverview: Boolean = false
        private set

    private var loaded = false
    private var dirty  = false

    private val cacheFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("glorpaddons")
        .resolve("storage_cache.nbt")
        .toFile()

    // ── Overview parsing ─────────────────────────────────────────────────

    fun updateFromOverview(slots: List<Slot>) {
        val found = mutableListOf<StoragePage>()
        for (i in 0..53) {
            val pageSlot = StoragePageSlot.fromOverviewSlot(i) ?: continue
            val stack = slots.getOrNull(i)?.stack ?: continue
            if (stack.isEmpty) continue
            if (isLockedPlaceholder(stack)) continue
            val name = stripFormatting(stack.name.string)
            if (name.isBlank()) continue
            found += StoragePage(pageSlot = pageSlot, icon = stack.copy(), name = name)
        }
        if (found.isNotEmpty()) {
            pages = found.sortedBy { it.pageSlot.index }
        }
        wasInOverview = true
        currentPage = null
    }

    // ── Page item caching ────────────────────────────────────────────────

    /** Cache items from a sub-page. Drops the first row (nav buttons). */
    fun cachePageItems(page: StoragePageSlot, slots: List<Slot>, rows: Int) {
        val items = (9 until (rows * 9)).map { i ->
            slots.getOrNull(i)?.stack?.copy() ?: ItemStack.EMPTY
        }
        pageItems[page.index] = items
        dirty = true
    }

    fun getPageItems(page: StoragePageSlot): List<ItemStack>? = pageItems[page.index]

    // ── State transitions ────────────────────────────────────────────────

    fun enterSubPage(page: StoragePageSlot) {
        currentPage = page
        wasInOverview = false
    }

    fun exitStorage() {
        currentPage = null
        wasInOverview = false
        if (dirty) {
            saveToDisk()
            dirty = false
        }
    }

    fun clearAll() {
        pages = emptyList()
        pageItems.clear()
        currentPage = null
        wasInOverview = false
        loaded = false
        dirty = false
    }

    // ── Persistence ─────────────────────────────────────────────────────

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!cacheFile.exists()) return
        val registries = MinecraftClient.getInstance().world?.registryManager ?: return
        val ops = registries.getOps(NbtOps.INSTANCE)

        try {
            val root = NbtIo.readCompressed(cacheFile.toPath(), NbtSizeTracker.ofUnlimitedBytes())
            for (key in root.keys) {
                val pageIndex = key.toIntOrNull() ?: continue
                val list = root.getListOrEmpty(key)
                val items = mutableListOf<ItemStack>()
                for (i in 0 until list.size) {
                    val compound = list.getCompoundOrEmpty(i)
                    if (compound.isEmpty) {
                        items.add(ItemStack.EMPTY)
                    } else {
                        val result = ItemStack.OPTIONAL_CODEC.parse(ops, compound)
                        items.add(result.result().orElse(ItemStack.EMPTY))
                    }
                }
                pageItems[pageIndex] = items
            }
            GlorpAddons.LOGGER.info("Loaded storage cache (${pageItems.size} pages)")
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to load storage cache", e)
        }
    }

    private fun saveToDisk() {
        val registries = MinecraftClient.getInstance().world?.registryManager ?: return
        val ops = registries.getOps(NbtOps.INSTANCE)

        try {
            val root = NbtCompound()
            for ((pageIndex, items) in pageItems) {
                val list = NbtList()
                for (stack in items) {
                    val encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack)
                        .result().orElse(NbtCompound())
                    list.add(encoded)
                }
                root.put(pageIndex.toString(), list)
            }
            cacheFile.parentFile.mkdirs()
            NbtIo.writeCompressed(root, cacheFile.toPath())
            GlorpAddons.LOGGER.info("Saved storage cache (${pageItems.size} pages)")
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to save storage cache", e)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Hypixel uses these items as placeholders for locked/empty storage pages. */
    private fun isLockedPlaceholder(stack: ItemStack): Boolean {
        val item = stack.item
        return item == Items.RED_STAINED_GLASS_PANE ||
               item == Items.BROWN_STAINED_GLASS_PANE ||
               item == Items.GRAY_DYE
    }

    fun stripFormatting(text: String): String =
        text.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "").trim()
}
