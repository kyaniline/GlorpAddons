package com.glorpaddons.equipment

import com.glorpaddons.GlorpAddons
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object EquipmentConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("glorpaddons")
        .resolve("equipment.json")
        .toFile()

    var config = EquipmentConfig()

    fun load() {
        try {
            if (configFile.exists()) {
                config = gson.fromJson(configFile.readText(), EquipmentConfig::class.java)
                    ?: EquipmentConfig()
            }
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to load equipment config", e)
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to save equipment config", e)
        }
    }
}
