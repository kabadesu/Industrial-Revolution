package me.steven.indrev.blocks

import alexiil.mc.lib.attributes.AttributeList
import alexiil.mc.lib.attributes.AttributeProvider
import alexiil.mc.lib.attributes.fluid.FluidAttributes
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume
import me.steven.indrev.blockentities.storage.TankBlockEntity
import me.steven.indrev.mixin.AccessorBucketItem
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.item.BucketItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.world.ServerWorld
import net.minecraft.stat.Stats
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import java.util.stream.Stream

class TankBlock(settings: Settings) : Block(settings), BlockEntityProvider, AttributeProvider {

    init {
        this.defaultState = stateManager.defaultState.with(UP, false).with(DOWN, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        builder?.add(UP, DOWN)
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? = TankBlockEntity()

    override fun getOutlineShape(
        state: BlockState,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        val isDown = state[DOWN]
        val isUp = state[UP]
        return when {
            !isUp && !isDown -> SINGLE_TANK_SHAPE
            isUp && !isDown -> TANK_UP_SHAPE
            !isUp && isDown -> TANK_DOWN_SHAPE
            else -> TANK_BOTH_SHAPE
        }
    }

    override fun onUse(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        val itemStack = player?.getStackInHand(hand)
        val item = itemStack?.item
        if (item is BucketItem) {
            val tankEntity = world?.getBlockEntity(pos) as? TankBlockEntity ?: return ActionResult.PASS
            val bucketFluid = (item as AccessorBucketItem).fluid
            val tank = tankEntity.fluidComponent.tanks[0]
            if (tank.volume.amount() >= FluidAmount.BUCKET && bucketFluid == Fluids.EMPTY) {
                val bucket = tank.volume.fluidKey.rawFluid?.bucketItem
                val extractable = FluidAttributes.EXTRACTABLE.get(world, pos)
                val volume = tank.volume.fluidKey.withAmount(FluidAmount.BUCKET)
                if (bucket != null && !extractable.extract(volume.amount()).isEmpty) {
                    itemStack.decrement(1)
                    player.inventory?.insertStack(ItemStack(bucket))
                    return ActionResult.SUCCESS
                }
            } else if (bucketFluid != Fluids.EMPTY) {
                val volume = FluidKeys.get(bucketFluid).withAmount(FluidAmount.BUCKET)
                val insertable = FluidAttributes.INSERTABLE.get(world, pos)
                if (insertable.insert(volume).isEmpty) {
                    itemStack.decrement(1)
                    player.inventory?.insertStack(ItemStack(Items.BUCKET))
                    return ActionResult.SUCCESS
                }
            }
        }
        return super.onUse(state, world, pos, player, hand, hit)
    }

    override fun afterBreak(
        world: World?,
        player: PlayerEntity?,
        pos: BlockPos?,
        state: BlockState?,
        blockEntity: BlockEntity?,
        toolStack: ItemStack?
    ) {
        player?.incrementStat(Stats.MINED.getOrCreateStat(this))
        player?.addExhaustion(0.005f)
        toTagComponents(world, player, pos, state, blockEntity, toolStack)
    }

    fun toTagComponents(
        world: World?,
        player: PlayerEntity?,
        pos: BlockPos?,
        state: BlockState?,
        blockEntity: BlockEntity?,
        toolStack: ItemStack?
    ) {
        if (world is ServerWorld) {
            getDroppedStacks(state, world, pos, blockEntity, player, toolStack).forEach { stack ->
                if (blockEntity is TankBlockEntity) {
                    val tag = stack.orCreateTag
                    blockEntity.fluidComponent.toTag(tag)
                }
                dropStack(world, pos, stack)
            }
            state!!.onStacksDropped(world, pos, toolStack)
        }
    }

    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction?,
        newState: BlockState?,
        world: WorldAccess?,
        pos: BlockPos?,
        posFrom: BlockPos?
    ): BlockState {
        val isTank = newState?.block is TankBlock
        return when (direction) {
            Direction.UP -> state.with(UP, isTank)
            Direction.DOWN -> state.with(DOWN, isTank)
            else -> super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom)
        }
    }

    override fun onPlaced(
        world: World?,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        val tag = itemStack?.tag
        if (tag?.isEmpty == false) {
            val tankEntity = world?.getBlockEntity(pos) as? TankBlockEntity ?: return
            tankEntity.fluidComponent.fromTag(tag)
        }
    }

    override fun appendTooltip(
        stack: ItemStack?,
        world: BlockView?,
        tooltip: MutableList<Text>?,
        options: TooltipContext?
    ) {
        val tag = stack?.tag
        val tanksTag = tag?.getCompound("tanks") ?: return
        val volume = tanksTag.keys?.map { key ->
            val tankTag = tanksTag.getCompound(key)
            FluidVolume.fromTag(tankTag.getCompound("fluids"))
        }?.firstOrNull() ?: return
        val fluid = volume.amount().asInt(1000)
        tooltip?.addAll(volume.fluidKey.fullTooltip.toTypedArray())
        tooltip?.add(LiteralText("$fluid / 8000 mB"))
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val blockPos = ctx.blockPos
        val world = ctx.world
        return defaultState
            .with(UP, world.testBlockState(blockPos.up()) { state -> state.block is TankBlock })
            .with(DOWN, world.testBlockState(blockPos.down()) { state -> state.block is TankBlock })
    }


    override fun addAllAttributes(world: World?, pos: BlockPos?, state: BlockState, to: AttributeList<*>?) {
        val tankEntity = world?.getBlockEntity(pos) as? TankBlockEntity ?: return
        val fluidComponent = tankEntity.fluidComponent
        val volume = fluidComponent.tanks[0].volume
        when (to?.attribute) {
            FluidAttributes.EXTRACTABLE -> {
                if (volume.isEmpty && state[DOWN]) {
                    var currentPos = pos?.down()
                    var currentState = world.getBlockState(currentPos)
                    while (currentState.block is TankBlock && currentState[DOWN]) {
                        val extractable = FluidAttributes.EXTRACTABLE.get(world, currentPos)
                        if (extractable !is EmptyFluidExtractable) {
                            to?.offer(extractable)
                            break
                        }
                        if (!currentState[DOWN]) break
                        currentPos = currentPos?.down()
                        currentState = world.getBlockState(currentPos)
                    }
                } else
                    to?.offer(fluidComponent)
            }
            FluidAttributes.INSERTABLE -> {
                if (fluidComponent.limit <= volume.amount() && state[UP]) {
                    var currentPos = pos?.up()
                    var currentState = world.getBlockState(currentPos)
                    while (currentState.block is TankBlock) {
                        val insertable = FluidAttributes.INSERTABLE.get(world, currentPos)
                        if (insertable !is RejectingFluidInsertable) {
                            to?.offer(insertable)
                            break
                        }
                        if (!currentState[UP]) break
                        currentPos = currentPos?.up()
                        currentState = world.getBlockState(currentPos)
                    }
                } else
                    to?.offer(fluidComponent)
            }
        }
    }

    companion object {
        val UP = BooleanProperty.of("up")
        val DOWN = BooleanProperty.of("down")
        
        val SINGLE_TANK_SHAPE = Stream.of(
            createCuboidShape(1.0, 14.0, 1.0, 15.0, 15.0, 15.0),
            createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.0, 15.0),
            createCuboidShape(14.0, 1.0, 14.0, 15.0, 14.0, 15.0),
            createCuboidShape(1.0, 1.0, 14.0, 2.0, 14.0, 15.0),
            createCuboidShape(1.0, 1.0, 1.0, 2.0, 14.0, 2.0),
            createCuboidShape(14.0, 1.0, 1.0, 15.0, 14.0, 2.0),
            createCuboidShape(14.5, 1.0, 1.5, 14.700000000000001, 14.0, 14.5),
            createCuboidShape(1.5, 1.0, 1.5, 1.700000000000001, 14.0, 14.5),
            createCuboidShape(1.5, 1.0, 1.5, 14.5, 14.0, 1.7),
            createCuboidShape(1.5, 1.0, 14.5, 14.5, 14.0, 14.7)
        ).reduce { v1, v2 -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR) }.get()

        val TANK_DOWN_SHAPE = Stream.of(
            createCuboidShape(1.0, 14.0, 1.0, 15.0, 15.0, 15.0),
            createCuboidShape(14.0, 0.0, 14.0, 15.0, 15.0, 15.0),
            createCuboidShape(1.0, 0.0, 14.0, 2.0, 15.0, 15.0),
            createCuboidShape(1.0, 0.0, 1.0, 2.0, 15.0, 2.0),
            createCuboidShape(14.0, 0.0, 1.0, 15.0, 15.0, 2.0),
            createCuboidShape(14.5, 0.0, 1.5, 14.7, 15.0, 14.5),
            createCuboidShape(1.5, 0.0, 1.5, 1.7, 15.0, 14.5),
            createCuboidShape(1.5, 0.0, 1.5, 14.5, 15.0, 1.7),
            createCuboidShape(1.5, 0.0, 14.5, 14.5, 15.0, 14.7)
        ).reduce { v1, v2 -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR) }.get()

        val TANK_UP_SHAPE = Stream.of(
            createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.0, 15.0),
            createCuboidShape(14.0, 1.0, 14.0, 15.0, 16.0, 15.0),
            createCuboidShape(1.0, 1.0, 14.0, 2.0, 16.0, 15.0),
            createCuboidShape(1.0, 1.0, 1.0, 2.0, 16.0, 2.0),
            createCuboidShape(1.04, 1.0, 1.0, 15.0, 16.0, 2.0),
            createCuboidShape(14.5, 1.0, 1.5, 14.7, 16.0, 14.5),
            createCuboidShape(1.5, 1.0, 1.5, 1.7, 16.0, 14.5),
            createCuboidShape(1.5, 1.0, 1.5, 14.5, 16.0, 1.7),
            createCuboidShape(1.5, 1.0, 14.5, 14.5, 16.0, 14.7)
        ).reduce { v1, v2 -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR) }.get()

        val TANK_BOTH_SHAPE = Stream.of(
            createCuboidShape(14.0, 0.0, 14.0, 15.0, 16.0, 15.0),
            createCuboidShape(1.0, 0.0, 14.0, 2.0, 16.0, 15.0),
            createCuboidShape(1.0, 0.0, 1.0, 2.0, 16.0, 2.0),
            createCuboidShape(14.0, 0.0, 1.0, 15.0, 16.0, 2.0),
            createCuboidShape(14.5, 0.0, 1.5, 14.7, 16.0, 14.5),
            createCuboidShape(1.5, 0.0, 1.5, 1.7, 16.0, 14.5),
            createCuboidShape(1.5, 0.0, 1.5, 14.5, 16.0, 1.7),
            createCuboidShape(1.5, 0.0, 14.5, 14.5, 16.0, 14.7)
        ).reduce { v1, v2 -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR) }.get()
    }
}