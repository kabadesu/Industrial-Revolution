package me.steven.indrev.gui.widgets.misc

import io.github.cottonmc.cotton.gui.widget.WWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import java.util.*

class WTip(random: Random) : WWidget() {
    val tip = getRandomTip(random)
    override fun paint(matrices: MatrixStack?, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val textWidth = MinecraftClient.getInstance().textRenderer.getWidth(tip)
        val offset = parent!!.width / 2 - textWidth / 2
        val client = MinecraftClient.getInstance()
        client.currentScreen?.renderTooltip(matrices, tip, x + offset, y)
    }

    companion object {
        fun getRandomTip(random: Random?): Text =
            TranslatableText("gui.indrev.tip").formatted(Formatting.GOLD)
                .append(TranslatableText("gui.indrev.tip_${random?.nextInt(10)}").formatted(Formatting.WHITE))
    }
}