package com.mojang.minecraft.gui;

import org.lwjgl.opengl.GL11;

import net.peyton.eagler.level.LevelUtils;

public final class GameOverScreen extends GuiScreen {

	public final void onOpen() {
		this.buttons.clear();
		this.buttons.add(new Button(0, this.width / 2 - 100, this.height / 4 + 72, "Get Softlocked Nerd!"));
		((Button) this.buttons.get(1)).active = false;
		LevelUtils.save();
	}

	protected final void onButtonClick(Button var1) {
		if (var1.id == 0) {
			this.minecraft.setCurrentScreen(new OptionsScreen(this, this.minecraft.settings));
		}

		if (var1.id == 1) {
			this.minecraft.setCurrentScreen(new GenerateLevelScreen(this));
		}
	}

	public final void render(int var1, int var2) {
		drawFadingBox(0, 0, this.width, this.height, 1615855616, -1602211792);
		GL11.glPushMatrix();
		GL11.glScalef(2.0F, 2.0F, 2.0F);
		drawCenteredString(this.fontRenderer, "Work In Progress!", this.width / 2 / 2, 30, 16777215);
		GL11.glPopMatrix();
		drawCenteredString(this.fontRenderer, "Your an Yee!", this.width / 2, 100,
				16777215);
		super.render(var1, var2);
	}
}
