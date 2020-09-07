package me.steven.indrev.compat.rei.categories

import it.unimi.dsi.fastutil.ints.IntList
import me.shedaniel.math.Point
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.EntryStack
import me.shedaniel.rei.api.TransferRecipeCategory
import me.shedaniel.rei.api.widgets.Widgets
import me.shedaniel.rei.gui.widget.Widget
import me.steven.indrev.compat.rei.plugins.FluidInfuserMachinePlugin
import me.steven.indrev.recipes.machines.FluidInfuserRecipe
import me.steven.indrev.utils.createREIFluidWidget
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier

class FluidInfuserRecipeCategory(
    private val identifier: Identifier,
    private val logo: EntryStack,
    private val categoryName: String
) : TransferRecipeCategory<FluidInfuserMachinePlugin> {

    override fun getIdentifier(): Identifier = identifier

    override fun getLogo(): EntryStack = logo

    override fun renderRedSlots(
        matrices: MatrixStack,
        widgets: MutableList<Widget>?,
        bounds: Rectangle,
        p3: FluidInfuserMachinePlugin?,
        redSlots: IntList
    ) {
        val startPoint = Point(bounds.centerX - 41, bounds.centerY - 27)
        matrices.push()
        matrices.translate(0.0, 0.0, 400.0)
        if (redSlots.contains(0)) {
            DrawableHelper.fill(
                matrices,
                startPoint.x + 1,
                startPoint.y + 1,
                startPoint.x + 1 + 16,
                startPoint.y + 1 + 16,
                1090453504
            )
        }
        matrices.pop()
    }

    override fun setupDisplay(recipeDisplay: FluidInfuserMachinePlugin, bounds: Rectangle): MutableList<Widget> {
        val startPoint = Point(bounds.centerX - 41, bounds.centerY - 27)
        val widgets = super.setupDisplay(recipeDisplay, bounds).toMutableList()
        widgets.add(Widgets.createArrow(Point(startPoint.x + 24, startPoint.y + 18)))
        val input = recipeDisplay.inputEntries
        widgets.add(Widgets.createSlot(Point(startPoint.x + 1, startPoint.y + 19)).entries(input[0]))
        widgets.add(
            Widgets.createSlot(Point(startPoint.x + 59, startPoint.y + 19)).entries(recipeDisplay.outputEntries)
        )
        val inputFluidPoint = Point(startPoint.x - 20, startPoint.y)
        createREIFluidWidget(widgets, inputFluidPoint, (recipeDisplay.recipe as FluidInfuserRecipe).inputFluid)
        val outputFluidPoint = Point(startPoint.x + 80, startPoint.y)
        createREIFluidWidget(widgets, outputFluidPoint, recipeDisplay.recipe.outputFluid)
        return widgets
    }

    override fun getCategoryName(): String = categoryName
}