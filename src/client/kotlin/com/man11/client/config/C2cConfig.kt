package com.man11.client.config

import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.sounds.SoundEvents
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import org.slf4j.LoggerFactory

data class Trigger(
	var phrase: String = "",
	var action: String = "",
	var delayTicks: Int = 0,
	var enabled: Boolean = true,
	var server: String = "",
	var requireConfirmation: Boolean = false,
	var commands: MutableList<TriggerAction> = mutableListOf()
)

data class TriggerAction(
	var command: String = "",
	var delaySeconds: Int = 0
)

object C2cConfig {
	private val logger = LoggerFactory.getLogger("c2c")
	private val gson = GsonBuilder().setPrettyPrinting().create()
	private val pending = ConcurrentLinkedQueue<PendingAction>()
	private var configPath: Path? = null
	val triggers: MutableList<Trigger> = mutableListOf()
	private var confirmationProgress = 0
	private var lastShiftTick = -1000
	private var lastConfirmationTickSound = -1000
	private var tickCounter = 0
	private data class PendingAction(
		val action: String,
		var ticks: Int,
		var confirmed: Boolean = false,
		var lastDisplayedSecond: Int = -1
	)

	fun confirmationProgress(): Int = confirmationProgress

	fun registerShiftPress() {
		if (!pending.any { !it.confirmed }) return
		if (tickCounter - lastShiftTick > 12) confirmationProgress = 0
		lastShiftTick = tickCounter
		confirmationProgress = (confirmationProgress + 1).coerceAtMost(3)
		Minecraft.getInstance().player?.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.2f)
		showConfirmationProgress()
		if (confirmationProgress >= 3) {
			confirmationProgress = 0
			lastShiftTick = -1000
			lastConfirmationTickSound = -1000
			pending.forEach { it.confirmed = true }
			Minecraft.getInstance().player?.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f)
			Minecraft.getInstance().player?.sendOverlayMessage(Component.literal("C2C Confirmed!"))
		}
	}

	private fun showConfirmationProgress() {
		val filledCount = if (confirmationProgress >= 3) 10 else confirmationProgress * 3
		val filled = "I".repeat(filledCount)
		val empty = "I".repeat(10 - filledCount)
		Minecraft.getInstance().player?.sendOverlayMessage(
			Component.literal("[")
				.append(Component.literal(filled).withStyle(ChatFormatting.GREEN))
				.append(Component.literal(empty).withStyle(ChatFormatting.GRAY))
				.append(Component.literal("] Press Shift rapidly to confirm"))
		)
	}

	fun load() {
		configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("c2c.json")
		val path = configPath ?: return
		if (Files.exists(path)) {
			runCatching {
				val loaded = gson.fromJson(Files.readString(path), Array<Trigger>::class.java)
				triggers.clear()
				if (loaded != null) {
					loaded.forEach { trigger ->
						if (trigger.commands.isEmpty() && trigger.action.isNotBlank()) {
							trigger.commands.add(TriggerAction(trigger.action, (trigger.delayTicks / 20).coerceAtLeast(0)))
						}
						if (trigger.commands.isEmpty()) trigger.commands.add(TriggerAction())
					}
					triggers.addAll(loaded)
				}
			}.onFailure { error -> logger.warn("Could not load c2c.json", error) }
		}
	}

	fun save() {
		val path = configPath ?: return
		Files.createDirectories(path.parent)
		Files.writeString(path, gson.toJson(triggers))
	}

	fun match(message: String) {
		val client = Minecraft.getInstance()
		val server = client.currentServer?.ip ?: "singleplayer"
		var confirmationRequested = false
		triggers.filter {
			it.enabled && it.phrase.isNotBlank() &&
				(it.server.isBlank() || it.server.equals(server, ignoreCase = true)) &&
				message.contains(it.phrase, ignoreCase = false)
		}
			.forEach { trigger ->
			trigger.commands.forEach { command ->
				if (command.command.isNotBlank()) {
					if (trigger.requireConfirmation) confirmationRequested = true
						pending.add(PendingAction(command.command, (command.delaySeconds * 20).coerceAtLeast(0), !trigger.requireConfirmation))
					}
				}
				if (confirmationRequested) {
				confirmationProgress = 0
				lastShiftTick = tickCounter
				lastConfirmationTickSound = tickCounter
				client.player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.0f)
				showConfirmationProgress()
				}
			}
	}

	fun tick(client: Minecraft) {
		tickCounter++
		val waitingForConfirmation = pending.any { !it.confirmed }
		if (waitingForConfirmation) {
			if (tickCounter - lastShiftTick >= 100) {
				pending.removeIf { !it.confirmed }
				confirmationProgress = 0
				lastShiftTick = -1000
				lastConfirmationTickSound = -1000
				client.player?.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.7f)
				client.player?.sendOverlayMessage(
					Component.literal("(C2C) ")
						.withStyle(ChatFormatting.WHITE)
						.append(Component.literal("Cancelled").withStyle(ChatFormatting.RED))
				)
			} else if (tickCounter - lastConfirmationTickSound >= 20) {
				lastConfirmationTickSound = tickCounter
				client.player?.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.45f, 1.4f)
			}
		}
		val ready = mutableListOf<PendingAction>()
		pending.forEach { pendingAction ->
			if (!pendingAction.confirmed) return@forEach
			if (pendingAction.ticks > 0) {
				pendingAction.ticks--
				val seconds = (pendingAction.ticks + 19) / 20
				if (seconds != pendingAction.lastDisplayedSecond) {
					pendingAction.lastDisplayedSecond = seconds
					showDelayProgress(pendingAction.action, seconds)
				}
			} else {
				ready += pendingAction
			}
		}
		for (pendingAction in ready) {
			if (!pending.remove(pendingAction)) continue
			val player = client.player ?: continue
			val action = pendingAction.action
			player.sendOverlayMessage(
				Component.literal("(C2C) ").withStyle(ChatFormatting.WHITE)
					.append(Component.literal("Sending").withStyle(ChatFormatting.GREEN))
			)
			when {
				action.startsWith("notify:", ignoreCase = true) ->
					player.sendSystemMessage(Component.literal(action.substringAfter(':').trim()))
				action.startsWith("title:", ignoreCase = true) ->
					player.sendSystemMessage(Component.literal(action.substringAfter(':').trim()))
				action.startsWith("sound:", ignoreCase = true) ->
					player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f)
				action.startsWith("/") ->
					player.connection.sendCommand(action.removePrefix("/"))
				else ->
					player.connection.sendChat(action)
			}
		}

	}

	private fun showDelayProgress(action: String, seconds: Int) {
		Minecraft.getInstance().player?.sendOverlayMessage(
			Component.literal("(C2C) ").withStyle(ChatFormatting.WHITE)
				.append(Component.literal(action).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" in ${seconds}s").withStyle(ChatFormatting.WHITE))
		)
	}

	fun sendPreviewError(message: String) {
		Minecraft.getInstance().player?.sendSystemMessage(Component.literal(message))
	}
}
