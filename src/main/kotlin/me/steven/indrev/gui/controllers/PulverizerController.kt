package me.steven.indrev.gui.controllers

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.blockentities.crafters.UpgradeProvider
import me.steven.indrev.gui.widgets.EnergyWidget
import me.steven.indrev.gui.widgets.ProcessWidget
import me.steven.indrev.gui.widgets.StringWidget
import me.steven.indrev.gui.widgets.TemperatureWidget
import me.steven.indrev.utils.add
import me.steven.indrev.utils.identifier
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext

class PulverizerController(syncId: Int, playerInventory: PlayerInventory, screenHandlerContext: ScreenHandlerContext) :
    SyncedGuiDescription(
        syncId,
        playerInventory,
        getBlockInventory(screenHandlerContext),
        getBlockPropertyDelegate(screenHandlerContext)
    ) {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(150, 120)

        root.add(StringWidget(I18n.translate("block.indrev.pulverizer"), titleColor), 4, 0)
        root.add(createPlayerInventoryPanel(), 0, 5)

        root.add(EnergyWidget(propertyDelegate), 0, 0, 16, 64)

        val batterySlot = WItemSlot.of(blockInventory, 0)
        root.add(batterySlot, 0.0, 3.7)

        val inputSlot = WItemSlot.of(blockInventory, 2)
        root.add(inputSlot, 2.3, 1.5)

        val processWidget = ProcessWidget(propertyDelegate)
        root.add(processWidget, 3.5, 1.5)

        val outputSlot = WItemSlot.outputOf(blockInventory, 3)
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 5.5, 1.5)

        val extraOutputSlot = WItemSlot.of(blockInventory, 4)
        extraOutputSlot.isInsertingAllowed = false
        root.add(extraOutputSlot, 5.5, 2.8)

        screenHandlerContext.run { world, pos ->
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is UpgradeProvider) {
                for ((i, slot) in blockEntity.getUpgradeSlots().withIndex()) {
                    val s = WItemSlot.of(blockInventory, slot)
                    root.add(s, 8, i)
                }
            }
            if (blockEntity is MachineBlockEntity && blockEntity.temperatureController != null) {
                val controller = blockEntity.temperatureController!!
                root.add(TemperatureWidget(propertyDelegate, controller), 1, 0, 16, 64)
                val coolerSlot = WItemSlot.of(blockInventory, 1)
                root.add(coolerSlot, 1.0, 3.7)
            }
        }

        root.validate(this)
    }

    override fun canUse(player: PlayerEntity?): Boolean = true

    companion object {
        val SCREEN_ID = identifier("pulverizer_screen")
    }
}