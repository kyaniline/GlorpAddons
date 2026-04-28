package com.glorpaddons.misc

import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

object PeekChat {

    @JvmStatic
    var peeking = false
        private set

    private var wasPeeking = false

    fun tick(client: MinecraftClient) {
        if (!MiscConfigManager.config.peekChatEnabled) {
            if (wasPeeking) {
                client.inGameHud.chatHud.resetScroll()
                wasPeeking = false
            }
            peeking = false
            return
        }

        val handle = client.window.handle
        val keyDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS
        peeking = keyDown && client.currentScreen == null

        if (wasPeeking && !peeking) {
            client.inGameHud.chatHud.resetScroll()
        }
        wasPeeking = peeking
    }
}
