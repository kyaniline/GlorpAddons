package com.glorpaddons.storage

import com.glorpaddons.GlorpAddons
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object StorageConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("glorpaddons")
        .resolve("storage.json")
        .toFile()

    var config = StorageConfig()

    fun load() {
        try {
            if (configFile.exists()) {
                config = gson.fromJson(configFile.readText(), StorageConfig::class.java)
                    ?: StorageConfig()
            }
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to load storage config", e)
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to save storage config", e)
        }
    }
}
