package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerBatteryMK3;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.storage.TileEntityMachineBatteryMK3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIMachineBatteryMK3 extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_battery.png");
	private TileEntityMachineBatteryMK3 battery;

	public GUIMachineBatteryMK3(InventoryPlayer invPlayer, TileEntityMachineBatteryMK3 battery) {
		super(new ContainerBatteryMK3(invPlayer, battery));
		this.battery = battery;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 69 - 52, 16, 52, battery.power, battery.maxPower);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = I18n.format(this.battery.getInventoryName());
		this.fontRendererObj.drawString(name, 70 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(battery.power > 0) {
			int p = (int) (battery.power * 52 / battery.maxPower);
			drawTexturedModalRect(guiLeft + 80, guiTop + 69 - p, 176, 52 - p, 16, p);
		}
	}
}
