package com.glorpaddons

import com.glorpaddons.commissions.CommissionHud
import com.glorpaddons.equipment.EquipmentConfigManager
import com.glorpaddons.equipment.EquipmentTracker
import com.glorpaddons.farming.FarmingConfigManager
import com.glorpaddons.farming.FarmingHud
import com.glorpaddons.farming.GardenPlots
import com.glorpaddons.farming.LowerSensitivity
import com.glorpaddons.farming.PestHighlighter
import com.glorpaddons.farming.VisitorHelper
import com.glorpaddons.itemrarity.ItemRarityConfigManager
import com.glorpaddons.misc.MiscConfigManager
import com.glorpaddons.mobesp.BatEsp
import com.glorpaddons.mobesp.MobEspConfigManager
import com.glorpaddons.storage.StorageConfigManager
import com.glorpaddons.storage.StorageOverlay
import com.glorpaddons.commissions.CommissionTracker
import com.glorpaddons.commissions.ConfigManager
import com.glorpaddons.commissions.ConfigScreen
import com.mojang.brigadier.Command
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object GlorpAddons : ClientModInitializer {

    const val MOD_ID = "glorpaddons"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitializeClient() {
        ConfigManager.load()
        StorageConfigManager.load()
        MobEspConfigManager.load()
        EquipmentConfigManager.load()
        ItemRarityConfigManager.load()
        MiscConfigManager.load()
        FarmingConfigManager.load()
        CommissionHud.register()
        StorageOverlay.register()
        BatEsp.register()
        FarmingHud.register()
        VisitorHelper.register()
        GardenPlots.register()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            CommissionTracker.tick()
            EquipmentTracker.tick(client)
            LowerSensitivity.tick(client)
            PestHighlighter.tick(client)
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("ga")
                    .executes {
                        val client = MinecraftClient.getInstance()
                        client.send { client.setScreen(ConfigScreen()) }
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        ClientCommandManager.literal("debug")
                            .executes { ctx ->
                                dumpTabList(ctx.source.client)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
        }

        LOGGER.info("GlorpAddons initialized!")
    }

    private fun dumpTabList(client: MinecraftClient) {
        val player = client.player ?: return
        val lines = CommissionTracker.getTabListLines(client)

        if (lines.isEmpty()) {
            player.sendMessage(Text.literal("[GA] Tab list is empty or unavailable."), false)
            return
        }

        player.sendMessage(Text.literal("[GA] Tab list (${lines.size} entries):"), false)
        lines.forEachIndexed { i, line ->
            player.sendMessage(Text.literal("[GA] §7$i: §f\"$line\""), false)
        }
    }
}
