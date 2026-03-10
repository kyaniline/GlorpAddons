package com.glorpaddons

import com.glorpaddons.commissions.CommissionHud
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
        CommissionHud.register()
        StorageOverlay.register()

        ClientTickEvents.END_CLIENT_TICK.register {
            CommissionTracker.tick()
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
