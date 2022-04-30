package net.fabricmc.example.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Selection extends WButton {
    private static final Identifier BUTTON_TEXTURES = new Identifier("minecraft", "textures/gui/container/enchanting_table.png");
	public Text subText;
	public List<Icon> icons = new ArrayList<>();

    /**
	 * Constructs a button with no label and no icon.
	 */
	public Selection() {
		
	}

	/**
	 * Constructs a button with an icon.
	 *
	 * @param icon the icon
	 * @since 2.2.0
	 */
	public Selection(Icon icon) {
		super(icon);
		this.icons.add(icon);
	}

	/**
	 * Constructs a button with a label.
	 *
	 * @param label the label
	 */
	public Selection(Text label) {
		super(label);
	}

	/**
	 * Constructs a button with an icon and a label.
	 *
	 * @param icon  the icon
	 * @param label the label
	 * @since 2.2.0
	 */
	public Selection(Icon icon, Text label) {
        super(icon, label);
	}
    
    @Environment(EnvType.CLIENT)
	@Override
	public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
		boolean hovered = (mouseX >= 0 && mouseY >= 0 && mouseX < getWidth() && mouseY < getHeight());
		int state = 1; //1=regular. 2=hovered. 0=disabled.
		if (!this.isEnabled()) {
			state = 0;
		} else if (hovered || isFocused()) {
			state = 2;
		}
		
		float px = 1/256f;
        int height = 19;
        int spriteWidth = 108;
		float buttonLeft = 0 * px;
		float buttonTop = (147 + (state * 19)) * px;
		int halfWidth = getWidth() / 2;
		if (halfWidth > spriteWidth) {
			halfWidth = spriteWidth;
		}

		float buttonWidth = halfWidth*px;
		float buttonHeight = height * px;
		
		float buttonEndLeft = (spriteWidth-(getWidth()/2)) * px;

		Identifier texture = BUTTON_TEXTURES;
		ScreenDrawing.texturedRect(matrices, x, y, getWidth()/2, height, texture, buttonLeft, buttonTop, buttonLeft+buttonWidth, buttonTop+buttonHeight, 0xFFFFFFFF);
		ScreenDrawing.texturedRect(matrices, x+(getWidth()/2), y, getWidth()/2, height, texture, buttonEndLeft, buttonTop, spriteWidth*px, buttonTop+buttonHeight, 0xFFFFFFFF);

        // TODO: make a list of icons?
        // Icon icon = this.getIcon();
		// if (icon != null) {
		// 	icon.paint(matrices, x + 1, y + 1, 16);
		// }

		for (int i = 0; i < this.icons.size(); i++) {
			Icon icon = this.icons.get(i);
			int pxShift = i * 16;
			icon.paint(matrices, x + 2 + pxShift, y + 2, 16);
		}
		
        Text label = this.getLabel();
		if (label!=null) {
			int color = 0xE0E0E0;
			if (!this.isEnabled()) {
				color = 0xA0A0A0;
			} /*else if (hovered) {
				color = 0xFFFFA0;
			}*/

			int xOffset = alignment == HorizontalAlignment.LEFT ? icons.size() * 18 + 2 : 4;
            xOffset = alignment == HorizontalAlignment.RIGHT ? -6 : xOffset;
			ScreenDrawing.drawStringWithShadow(matrices, label.asOrderedText(), alignment, x + xOffset, y + ((20 - 8) / 2), width, color);
		}

		if (this.subText != null) {
			int color = 0x9DCD19;
			// this.subText
			ScreenDrawing.drawStringWithShadow(matrices, this.subText.asOrderedText(), HorizontalAlignment.RIGHT, x - 2, y + 9, width, color);
		}
	}

	public void addIcon(Icon icon) {
		this.icons.add(icon);
	}

	public void setSubText(Text subText) {
		this.subText = subText;
	}
}
