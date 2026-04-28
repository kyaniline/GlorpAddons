package com.glorpaddons.mobesp

import com.glorpaddons.GlorpAddons
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object MobEspConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("glorpaddons")
        .resolve("mobesp.json")
        .toFile()

    var config = MobEspConfig()

    fun load() {
        try {
            if (configFile.exists()) {
                config = gson.fromJson(configFile.readText(), MobEspConfig::class.java)
                    ?: MobEspConfig()
            }
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to load mob esp config", e)
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            GlorpAddons.LOGGER.error("Failed to save mob esp config", e)
        }
    }
}
