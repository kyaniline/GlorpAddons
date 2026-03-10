package com.glorpaddons.commissions

import com.glorpaddons.GlorpAddons
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object ConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("glorpaddons")
        .resolve("commissions.json")
        .toFile()

    var config = CommissionConfig()

    fun load() {
        try {
            if (configFile.exists()) {
                config = gson.fromJson(configFile.readText(), CommissionConfig::class.java)
                    ?: CommissionConfig()
            }
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to load commission config", e)
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to save commission config", e)
        }
    }
}
