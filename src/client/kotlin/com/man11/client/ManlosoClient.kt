package com.man11.client

import com.man11.Manloso
import com.man11.client.config.C2cConfig
import com.man11.client.screen.C2cSettingsScreen
import com.man11.client.screen.C2cProfileScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import org.lwjgl.glfw.GLFW

object ManlosoClient : ClientModInitializer {
	private lateinit var settingsKey: KeyMapping
	private var openSettingsNextTick = false
	private var shiftWasDown = false
	private val c2cCategory = KeyMapping.Category.register(Manloso.id("keybinds"))

	override fun onInitializeClient() {
		C2cConfig.load()
		settingsKey = KeyMappingHelper.registerKeyMapping(
			KeyMapping(
				"key.c2c.open_settings",
				GLFW.GLFW_KEY_KP_ENTER,
				c2cCategory
			)
		)

		ClientReceiveMessageEvents.GAME.register { message, _ ->
			C2cConfig.match(message.string)
		}

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				ClientCommands.literal("c2c")
					.executes {
						openSettingsNextTick = true
						1
					}
					.then(ClientCommands.literal("settings").executes {
						openSettingsNextTick = true
						1
					})
					.then(ClientCommands.literal("setting").executes {
						openSettingsNextTick = true
						1
					})
			)
		}

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			val shiftDown = GLFW.glfwGetKey(client.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
				GLFW.glfwGetKey(client.window.handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
			if (shiftDown && !shiftWasDown) C2cConfig.registerShiftPress()
			shiftWasDown = shiftDown
			if (settingsKey.consumeClick()) {
				openSettingsNextTick = true
			}
			if (openSettingsNextTick) {
				openSettingsNextTick = false
				client.setScreenAndShow(C2cProfileScreen())
			}
			C2cConfig.tick(client)
		}
	}
}