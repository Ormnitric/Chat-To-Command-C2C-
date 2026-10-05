package com.man11.client.screen

import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

class C2cProfileScreen : Screen(Component.literal("C2C Profiles")) {
	private val description = "Chat to Command (C2C) is a utility mod that detects specific chat messages and automatically executes assigned commands, fully configurable through an in-game UI."
	private val serverAddress: String
		get() = minecraft.currentServer?.ip ?: "singleplayer"
	private val serverName: String
		get() {
			if (serverAddress.startsWith("[")) {
				return serverAddress.substringAfter("[").substringBefore("]")
			}
			val separator = serverAddress.lastIndexOf(':')
			return if (separator > 0 && serverAddress.substring(separator + 1).all(Char::isDigit)) {
				serverAddress.substring(0, separator)
			} else {
				serverAddress
			}
		}

	override fun init() {
		val centerX = width / 2
		addRenderableOnly(StringWidget(centerX - font.width("Chat to Command (C2C)") / 2, height / 2 - 88, 220, 20,
			Component.literal("Chat to Command (C2C)").withStyle(ChatFormatting.AQUA), font))
		addRenderableOnly(StringWidget(centerX - font.width("Choose a profile to configure") / 2, height / 2 - 60, 220, 20,
			Component.literal("Choose a profile to configure"), font))
		addRenderableWidget(Button.builder(Component.literal("Global").withStyle(ChatFormatting.GOLD)) {
			minecraft.setScreenAndShow(C2cSettingsScreen(null, "Global"))
		}.bounds(centerX - 140, height / 2 - 20, 280, 28).build())
		addRenderableWidget(Button.builder(Component.literal("Server: $serverName").withStyle(ChatFormatting.AQUA)) {
			minecraft.setScreenAndShow(C2cSettingsScreen(serverAddress, serverName))
		}.bounds(centerX - 140, height / 2 + 22, 280, 28).build())
		addRenderableWidget(Button.builder(Component.literal("Cancel")) {
			C2cScreenHelper.close()
		}.bounds(centerX - 60, height / 2 + 64, 120, 24).build())
		addRenderableOnly(StringWidget(12, height - 38, width - 24, 20, Component.literal("* $description"), font))
	}

	override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		extractTransparentBackground(guiGraphics)
		guiGraphics.fill(0xDD10151B.toInt(), 0, height - 48, width, height)
		super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
	}

	override fun mouseMoved(mouseX: Double, mouseY: Double) {
		val hovered = children().firstOrNull { it.isMouseOver(mouseX, mouseY) }
		if (hovered != null && hovered != lastHovered) {
			minecraft.player?.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.6f)
		}
		lastHovered = hovered
		super.mouseMoved(mouseX, mouseY)
	}

	private var lastHovered: Any? = null
}
