package com.glorpaddons.farming

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object FarmingConfigManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir.resolve("glorpaddons/farming.json").toFile()

    var config: FarmingConfig = FarmingConfig()
        private set

    fun load() {
        if (configFile.exists()) {
            config = gson.fromJson(configFile.readText(), FarmingConfig::class.java) ?: FarmingConfig()
        }
    }

    fun save() {
        configFile.parentFile.mkdirs()
        configFile.writeText(gson.toJson(config))
    }
}
