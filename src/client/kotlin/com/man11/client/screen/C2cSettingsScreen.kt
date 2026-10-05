package com.man11.client.screen

import com.man11.client.config.C2cConfig
import com.man11.client.config.Trigger
import com.man11.client.config.TriggerAction
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

class C2cSettingsScreen(private val profileServer: String?, private val profileName: String) : Screen(Component.literal("Chat to Command (C2C) config")) {
	private val workingTriggers = C2cConfig.triggers.filter { it.server == (profileServer ?: "") }.map { copyTrigger(it) }.toMutableList()
	private val originalTriggers = workingTriggers.map { copyTrigger(it) }
	private val triggerRows = mutableListOf<TriggerRow>()
	private val expanded = mutableSetOf<Int>()
	private var scroll = 0
	private val listTop = 94
	private var listBottom = 0
	private var closing = false
	private var hoveredWidget: GuiEventListener? = null

	override fun init() {
		triggerRows.clear()
		listBottom = height - 54
		addRenderableOnly(StringWidget((width - font.width(title)) / 2, 20, font.width(title), 20, title, font))
		addRenderableOnly(StringWidget((width - font.width(Component.literal(profileName))) / 2, 46, font.width(Component.literal(profileName)), 20,
			Component.literal(profileName).withStyle(if (profileServer == null) ChatFormatting.GOLD else ChatFormatting.AQUA), font))
		addRenderableOnly(StringWidget(30, 76, 130, 20, Component.literal("Trigger"), font))
		addRenderableOnly(StringWidget(170, 76, 150, 20, Component.literal("ChatTrigger"), font))
		workingTriggers.forEachIndexed { index, trigger -> addTriggerRow(index, trigger) }
		addRenderableWidget(Button.builder(Component.literal("+")) {
			workingTriggers.add(Trigger(server = profileServer ?: "", commands = mutableListOf(TriggerAction())))
			expanded.add(workingTriggers.lastIndex)
			rebuildWidgets()
		}.bounds(width - 64, 70, 30, 24).build().also {
			it.setTooltip(Tooltip.create(Component.literal("Add a new Trigger")))
		})
		addRenderableWidget(Button.builder(Component.literal("Done")) {
			saveAndClose()
		}.bounds(width - 100, height - 36, 90, 24).build().also {
			it.setTooltip(Tooltip.create(Component.literal("Save changes and close")))
		})
		addRenderableWidget(Button.builder(Component.literal("Cancel")) {
			requestCancelAndClose()
		}.bounds(width - 194, height - 36, 86, 24).build().also {
			it.setTooltip(Tooltip.create(Component.literal("Discard changes and close")))
		})
	}

	private fun addTriggerRow(index: Int, trigger: Trigger) {
		val y = listTop + calculateOffset(index) - scroll
		addRenderableOnly(StringWidget(60, y + 8, 105, 20, Component.literal("Trigger ${index + 1}"), font))
		val header = Button.builder(Component.literal(if (expanded.contains(index)) "v" else ">")) {
			if (!expanded.add(index)) expanded.remove(index)
			rebuildWidgets()
		}.bounds(30, y + 4, 28, 24).build().also {
			it.setTooltip(Tooltip.create(Component.literal("Expand or collapse Trigger ${index + 1}")))
		}
		addRenderableWidget(header)

		val phrase = EditBox(font, 170, y + 4, width - 390, 22, Component.literal("Message to detect"))
		phrase.setMaxLength(512)
		phrase.value = trigger.phrase
		phrase.setTooltip(Tooltip.create(Component.literal("ChatTrigger: text that activates this Trigger")))
		addRenderableWidget(phrase)
		val enabled = Checkbox.builder(Component.empty(), font)
			.pos(width - 210, y + 6)
			.selected(trigger.enabled)
			.onValueChange { _, selected ->
			syncRows()
			trigger.enabled = selected
			}
			.build().also {
			it.setTooltip(Tooltip.create(Component.literal("Enable or disable this trigger")))
		}
		addRenderableWidget(enabled)
		addRenderableWidget(Button.builder(Component.literal("-")) {
			syncRows()
			workingTriggers.removeAt(index)
			expanded.remove(index)
			rebuildWidgets()
		}.bounds(width - 156, y + 2, 34, 28).build().also {
			it.setTooltip(Tooltip.create(Component.literal("Delete this Trigger")))
		})
		val row = TriggerRow(trigger, phrase, mutableListOf())
		triggerRows += row

		if (expanded.contains(index)) {
			trigger.commands.forEachIndexed { commandIndex, action ->
				val actionY = y + 50 + commandIndex * 34
				addRenderableOnly(StringWidget(78, actionY + 7, 68, 20, Component.literal("Command"), font))
				addRenderableOnly(StringWidget(width - 220, actionY - 15, 54, 20, Component.literal("Delay"), font))
				val command = EditBox(font, 170, actionY, width - 390, 22, Component.literal("Command or message"))
				command.setMaxLength(512)
				command.value = action.command
				command.setTooltip(Tooltip.create(Component.literal("Command, notify:text, title:text, or sound:any")))
				val delay = EditBox(font, width - 220, actionY, 54, 22, Component.literal("Seconds"))
				delay.value = action.delaySeconds.toString()
				delay.setTooltip(Tooltip.create(Component.literal("Delay: seconds to wait before sending")))
				addRenderableWidget(command)
				addRenderableWidget(delay)
				addRenderableWidget(Button.builder(Component.literal("-")) {
					syncRows()
					trigger.commands.removeAt(commandIndex)
					if (trigger.commands.isEmpty()) trigger.commands.add(TriggerAction())
					rebuildWidgets()
				}.bounds(width - 156, actionY - 2, 34, 28).build().also {
					it.setTooltip(Tooltip.create(Component.literal("Delete this command")))
				})
				row.commands += CommandRow(action, command, delay)
				}
			val confirmY = y + 50 + trigger.commands.size * 34
			addRenderableWidget(
				Checkbox.builder(Component.literal("Shift to Confirm"), font)
					.pos(78, confirmY + 4)
					.selected(trigger.requireConfirmation)
					.onValueChange { _, selected ->
						syncRows()
						trigger.requireConfirmation = selected
					}
					.build()
					.also {
						it.setTooltip(Tooltip.create(Component.literal("Require five rapid Shift presses before sending")))
					}
			)
			val addY = y + 48 + (trigger.commands.size - 1).coerceAtLeast(0) * 34
			addRenderableWidget(Button.builder(Component.literal("+")) {
				syncRows()
				trigger.commands.add(TriggerAction())
				rebuildWidgets()
			}.bounds(width - 112, addY - 2, 34, 28).build().also {
				it.setTooltip(Tooltip.create(Component.literal("Add another command")))
			})
		}
	}

	private fun calculateOffset(index: Int): Int {
		var offset = 0
		for (i in 0 until index) {
			offset += 38
			if (expanded.contains(i)) offset += 34 * workingTriggers[i].commands.size + 76
		}
		return offset
	}

	private fun syncRows() {
		triggerRows.forEach { row ->
			row.trigger.phrase = row.phrase.value.trim()
			row.commands.forEach { command ->
				command.action.command = command.input.value.trim()
				command.action.delaySeconds = command.delay.value.toIntOrNull()?.coerceAtLeast(0) ?: 0
			}
			row.trigger.action = row.trigger.commands.firstOrNull()?.command.orEmpty()
			row.trigger.delayTicks = (row.trigger.commands.firstOrNull()?.delaySeconds ?: 0) * 20
		}
	}

	private fun isDirty(): Boolean {
		syncRows()
		return workingTriggers != originalTriggers
	}

	private fun saveAndClose() {
		syncRows()
		C2cConfig.triggers.removeAll { it.server == (profileServer ?: "") }
		C2cConfig.triggers.addAll(workingTriggers.map { copyTrigger(it) })
		C2cConfig.save()
		closeScreen()
	}

	private fun requestSaveAndClose() {
		saveAndClose()
	}

	private fun requestCancelAndClose() {
		if (!isDirty()) {
			closeWithoutSaving()
			return
		}
		minecraft.setScreenAndShow(
			ConfirmScreen(
				{ discard -> if (discard) closeWithoutSaving() else minecraft.setScreenAndShow(this) },
				Component.literal("Discard changes?"),
				Component.literal("You have unsaved changes. Discard them?"),
				Component.literal("Discard"),
				Component.literal("Keep Editing")
			)
		)
	}

	private fun closeWithoutSaving() {
		closing = true
		closeScreen()
	}

	private fun closeScreen() {
		closing = true
		C2cScreenHelper.close()
	}

	override fun rebuildWidgets() {
		syncRows()
		clearWidgets()
		init()
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
		syncRows()
		val contentHeight = listTop + workingTriggers.indices.sumOf { index ->
			32 + if (expanded.contains(index)) 34 * workingTriggers[index].commands.size + 76 else 0
		}
		scroll = (scroll - scrollY.toInt() * 24).coerceIn(0, (contentHeight - listBottom).coerceAtLeast(0))
		rebuildWidgets()
		return true
	}

	override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		val currentHoveredWidget = children().firstOrNull { it is AbstractWidget && it.isHovered }
		if (currentHoveredWidget !== hoveredWidget) {
			if (currentHoveredWidget != null) {
				minecraft.player?.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.6f)
			}
			hoveredWidget = currentHoveredWidget
		}
		extractTransparentBackground(guiGraphics)
		guiGraphics.fill(0x880B0A10.toInt(), 18, 16, width - 18, height - 44)
		drawPanel(guiGraphics, 18, 16, width - 18, 68, 0xAA17151D.toInt())
		drawPanel(guiGraphics, 26, 72, width - 26, 92, 0x99232831.toInt())
		drawPanel(guiGraphics, 26, listBottom + 8, width - 26, height - 44, 0x66151A20)
		for (index in workingTriggers.indices) {
			val y = listTop + calculateOffset(index) - scroll
			val panelHeight = 32 + if (expanded.contains(index)) 34 * workingTriggers[index].commands.size + 62 else 0
			drawPanel(guiGraphics, 24, y, width - 24, y + panelHeight, 0x551F2630)
			if (expanded.contains(index)) {
				drawPanel(guiGraphics, 62, y + 38, width - 170, y + 42 + 34 * workingTriggers[index].commands.size, 0x4410171D)
				workingTriggers[index].commands.forEachIndexed { commandIndex, _ ->
				}
			}
		}
		drawScrollbar(guiGraphics)
		super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
	}

	private fun drawPanel(guiGraphics: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
		guiGraphics.fill(color, left, top, right, bottom)
		guiGraphics.fill(0x553F4A55, left, top, right, top + 2)
		guiGraphics.fill(0x553F4A55, left, bottom - 2, right, bottom)
		guiGraphics.fill(0x553F4A55, left, top, left + 2, bottom)
		guiGraphics.fill(0x553F4A55, right - 2, top, right, bottom)
	}

	private fun drawScrollbar(guiGraphics: GuiGraphicsExtractor) {
		val contentHeight = listTop + workingTriggers.indices.sumOf { index ->
			32 + if (expanded.contains(index)) 34 * workingTriggers[index].commands.size + 76 else 0
		}
		val maxScroll = (contentHeight - listBottom).coerceAtLeast(0)
		val trackTop = listTop
		val trackBottom = listBottom
		val trackHeight = (trackBottom - trackTop).coerceAtLeast(1)
		val thumbHeight = if (maxScroll == 0) trackHeight else
			(trackHeight * trackHeight / contentHeight.coerceAtLeast(trackHeight)).coerceIn(24, trackHeight)
		val thumbTop = if (maxScroll == 0) trackTop else
			trackTop + ((trackHeight - thumbHeight) * scroll / maxScroll)
		drawPanel(guiGraphics, width - 16, trackTop, width - 8, trackBottom, 0x66303840)
		drawPanel(guiGraphics, width - 15, thumbTop, width - 9, thumbTop + thumbHeight, 0xCC8A8A8A.toInt())
	}

	private fun drawSquare(guiGraphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, label: String) {
		guiGraphics.fill(0xFF8A8A8A.toInt(), x, y, x + width, y + height)
		guiGraphics.fill(0xFF202020.toInt(), x, y, x + width, y + 2)
		guiGraphics.fill(0xFF202020.toInt(), x, y + height - 2, x + width, y + height)
		guiGraphics.fill(0xFF202020.toInt(), x, y, x + 2, y + height)
		guiGraphics.fill(0xFF202020.toInt(), x + width - 2, y, x + width, y + height)
	}

	private fun addSquareLabel(x: Int, y: Int, width: Int, height: Int, label: String) {
		val textWidth = font.width(label)
		addRenderableOnly(StringWidget(x + (width - textWidth) / 2, y + (height - 9) / 2, textWidth, 12, Component.literal(label), font))
	}

	override fun onClose() {
		if (!closing) requestCancelAndClose()
	}

	private fun copyTrigger(trigger: Trigger) = trigger.copy(
		commands = trigger.commands.map { it.copy() }.toMutableList()
	)

	private data class TriggerRow(
		val trigger: Trigger,
		val phrase: EditBox,
		val commands: MutableList<CommandRow>
	)

	private data class CommandRow(
		val action: TriggerAction,
		val input: EditBox,
		val delay: EditBox
	)
}
