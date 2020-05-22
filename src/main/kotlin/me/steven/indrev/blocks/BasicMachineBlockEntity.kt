package me.steven.indrev.blocks

import io.github.cottonmc.cotton.gui.PropertyDelegateHolder
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.container.PropertyDelegate
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Tickable
import net.minecraft.util.math.Direction
import team.reborn.energy.EnergySide
import team.reborn.energy.EnergyStorage
import team.reborn.energy.EnergyTier

abstract class BasicMachineBlockEntity(type: BlockEntityType<*>, private val baseBuffer: Double) : BlockEntity(type), BlockEntityClientSerializable, EnergyStorage, PropertyDelegateHolder, Tickable {
    var energy = 0.0
        set(value) {
            propertyDelegate[0] = value.toInt()
            field = value.coerceAtMost(maxStoredPower)
        }
        get() {
            field = field.coerceAtMost(maxStoredPower)
            return field
        }
    private var delegate: PropertyDelegate? = null
        get() {
            if (field == null) field = createDelegate()
            return field
        }

    override fun tick() {
        if (world?.isClient == true) return

        val block = this.cachedState.block
        if (block !is BasicMachineBlock) return
        for (direction in Direction.values()) {
            val targetPos = pos.offset(direction)
            block.tryProvideEnergyTo(world, pos, targetPos)
            markDirty()
        }
    }

    fun takeEnergy(amount: Double): Boolean {
        return if (amount <= energy) {
            energy -= amount
            true
        } else false
    }

    fun addEnergy(amount: Double): Double {
        val added = (maxStoredPower - energy).coerceAtMost(amount)
        energy += added
        return added
    }

    protected abstract fun createDelegate(): PropertyDelegate

    override fun getPropertyDelegate(): PropertyDelegate {
        val delegate = this.delegate!!
        delegate[1] = maxStoredPower.toInt()
        return delegate
    }

    override fun setStored(amount: Double) {
        this.energy = amount
    }

    override fun getMaxStoredPower(): Double = baseBuffer

    abstract fun getMaxInput(): Double

    abstract fun getMaxOutput(): Double

    @Deprecated("unsupported")
    override fun getTier(): EnergyTier = throw UnsupportedOperationException()

    @Deprecated("use getMaxOutput() instead", ReplaceWith("getMaxOutput()"))
    override fun getMaxOutput(side: EnergySide?): Double = getMaxOutput()

    @Deprecated("use getMaxInput() instead!", ReplaceWith("getMaxInput()"))
    override fun getMaxInput(side: EnergySide?): Double = getMaxInput()

    override fun getStored(side: EnergySide?): Double {
        val direction = EnergySide.fromMinecraft(this.cachedState[BasicMachineBlock.FACING])
        if (direction == EnergySide.UNKNOWN || direction == side) return 0.0
        return energy
    }

    override fun fromTag(tag: CompoundTag?) {
        super.fromTag(tag)
        energy = tag?.getDouble("Energy") ?: 0.0
    }

    override fun toTag(tag: CompoundTag?): CompoundTag {
        tag?.putDouble("Energy", energy)
        return super.toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag?) {
        energy = tag?.getDouble("Energy") ?: 0.0
    }

    override fun toClientTag(tag: CompoundTag?): CompoundTag {
        if (tag == null) return CompoundTag()
        tag.putDouble("Energy", energy)
        return tag
    }
}