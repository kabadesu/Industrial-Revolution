package me.steven.indrev.blockentities.crafters

import me.steven.indrev.blockentities.InterfacedMachineBlockEntity
import me.steven.indrev.items.Upgrade
import me.steven.indrev.utils.Tier
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.container.ArrayPropertyDelegate
import net.minecraft.container.PropertyDelegate
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeFinder
import net.minecraft.recipe.RecipeInputProvider
import net.minecraft.util.Tickable
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorld
import team.reborn.energy.EnergySide
import kotlin.math.ceil

abstract class CraftingMachineBlockEntity<T : Recipe<Inventory>>(
        type: BlockEntityType<*>,
        tier: Tier,
        baseBuffer: Double
) :
        InterfacedMachineBlockEntity(type, tier, baseBuffer), Tickable, RecipeInputProvider, UpgradeProvider {
    var inventory: SidedInventory? = null
        get() = field ?: createInventory().apply { field = this }
    var processingItem: Item? = null
    var output: ItemStack? = null
    var processTime: Int = 0
        set(value) {
            field = value.apply { propertyDelegate[2] = this }
        }
        get() = field.apply { propertyDelegate[2] = this }
    var totalProcessTime: Int = 0
        set(value) {
            field = value.apply { propertyDelegate[3] = this }
        }
        get() = field.apply { propertyDelegate[3] = this }

    override fun tick() {
        super.tick()
        if (world?.isClient == true) return
        val inputStack = inventory!!.getInvStack(0)
        val outputStack = inventory!!.getInvStack(1).copy()
        if (isProcessing()) {
            if (inputStack.isEmpty) reset()
            else if (!inputStack.isEmpty && inputStack.item != processingItem)
                findRecipe(inventory!!)?.also { recipe ->
                    processingItem = inputStack.item
                    output = recipe.output
                } ?: reset()
            else if (inputStack.item == processingItem && takeEnergy(Upgrade.ENERGY.apply(this, inventory!!))) {
                processTime = (processTime - ceil(Upgrade.SPEED.apply(this, inventory!!)).toInt()).coerceAtLeast(0)
                if (processTime <= 0) {
                    inventory!!.setInvStack(0, inputStack.apply { count-- })
                    if (outputStack.item == output?.item)
                        inventory!!.setInvStack(1, outputStack.apply { increment(output?.count ?: 0) })
                    else if (outputStack.isEmpty)
                        inventory!!.setInvStack(1, output?.copy())
                    onCraft()
                    reset()
                }
            } else reset()
        } else if (energy > 0 && !inputStack.isEmpty && processTime <= 0) {
            reset()
            findRecipe(inventory!!)?.apply { startRecipe(this) }
        }
        markDirty()
    }

    abstract fun findRecipe(inventory: Inventory): T?

    abstract fun startRecipe(recipe: T)

    abstract fun createInventory(): SidedInventory

    private fun reset() {
        processTime = 0
        totalProcessTime = 0
        processingItem = null
        output = null
    }

    override fun getMaxStoredPower(): Double = Upgrade.BUFFER.apply(this, inventory!!)

    override fun createDelegate(): PropertyDelegate = ArrayPropertyDelegate(4)

    override fun getMaxOutput(side: EnergySide?): Double = 0.0

    private fun isProcessing() = processTime > 0 && energy > 0

    override fun fromTag(tag: CompoundTag?) {
        processTime = tag?.getInt("ProcessTime") ?: 0
        totalProcessTime = tag?.getInt("MaxProcessTime") ?: 0
        super.fromTag(tag)
    }

    override fun toTag(tag: CompoundTag?): CompoundTag {
        tag?.putInt("ProcessTime", processTime)
        tag?.putInt("MaxProcessTime", totalProcessTime)
        return super.toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag?) {
        processTime = tag?.getInt("ProcessTime") ?: 0
        totalProcessTime = tag?.getInt("MaxProcessTime") ?: 0
        super.fromClientTag(tag)
    }

    override fun toClientTag(tag: CompoundTag?): CompoundTag {
        tag?.putInt("ProcessTime", processTime)
        tag?.putInt("MaxProcessTime", totalProcessTime)
        return super.toClientTag(tag)
    }

    override fun getInventory(state: BlockState?, world: IWorld?, pos: BlockPos?): SidedInventory = inventory!!

    override fun provideRecipeInputs(recipeFinder: RecipeFinder?) {
        for (i in 0 until inventory!!.invSize)
            recipeFinder?.addItem(inventory!!.getInvStack(i))
    }

    open fun onCraft() {}
}