package me.steven.indrev.gui.widgets.misc

import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text

class WText(
    private val string: () -> Text,
    private val alignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    private val color: Int = -1
) : WWidget() {
    constructor(string: Text, alignment: HorizontalAlignment = HorizontalAlignment.CENTER, color: Int = -1) : this(
        { string },
        alignment,
        color
    )

    override fun paint(matrices: MatrixStack?, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        ScreenDrawing.drawString(matrices, string().asOrderedText(), alignment, x, y, this.width, color)
    }
}