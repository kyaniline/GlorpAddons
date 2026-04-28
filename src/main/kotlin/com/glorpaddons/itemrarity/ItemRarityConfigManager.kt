package com.glorpaddons.itemrarity

import com.glorpaddons.GlorpAddons
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object ItemRarityConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("glorpaddons")
        .resolve("itemrarity.json")
        .toFile()

    var config = ItemRarityConfig()

    fun load() {
        try {
            if (configFile.exists()) {
                config = gson.fromJson(configFile.readText(), ItemRarityConfig::class.java)
                    ?: ItemRarityConfig()
            }
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to load item rarity config", e)
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to save item rarity config", e)
        }
    }
}
