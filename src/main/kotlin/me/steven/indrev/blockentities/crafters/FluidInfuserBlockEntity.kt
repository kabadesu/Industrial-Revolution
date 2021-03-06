package me.steven.indrev.blockentities.crafters

import me.steven.indrev.components.FluidInfuserFluidComponent
import me.steven.indrev.components.InventoryComponent
import me.steven.indrev.components.TemperatureComponent
import me.steven.indrev.inventories.IRInventory
import me.steven.indrev.items.misc.IRCoolerItem
import me.steven.indrev.items.upgrade.IRUpgradeItem
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.recipes.machines.FluidInfuserRecipe
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.Tier
import net.minecraft.recipe.RecipeType
import team.reborn.energy.Energy

class FluidInfuserBlockEntity(tier: Tier) : CraftingMachineBlockEntity<FluidInfuserRecipe>(tier, MachineRegistry.FLUID_INFUSER_REGISTRY) {

    init {
        this.inventoryComponent = InventoryComponent({ this }) {
            IRInventory(8, intArrayOf(2), intArrayOf(3)) { slot, stack ->
                val item = stack?.item
                when {
                    item is IRUpgradeItem -> getUpgradeSlots().contains(slot)
                    Energy.valid(stack) && Energy.of(stack).maxOutput > 0 -> slot == 0
                    item is IRCoolerItem -> slot == 1
                    slot == 2 -> true
                    else -> false
                }
            }
        }
        this.fluidComponent = FluidInfuserFluidComponent({ this })
        this.temperatureComponent = TemperatureComponent({ this }, 0.06, 700..1100, 1400.0)
    }

    override val type: RecipeType<FluidInfuserRecipe> = FluidInfuserRecipe.TYPE


    override fun getUpgradeSlots(): IntArray = intArrayOf(4, 5, 6, 7)

    override fun getAvailableUpgrades(): Array<Upgrade> = Upgrade.DEFAULT
}