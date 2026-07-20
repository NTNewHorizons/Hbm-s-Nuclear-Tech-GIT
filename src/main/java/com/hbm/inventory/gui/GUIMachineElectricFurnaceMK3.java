package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerElectricFurnaceMK3;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnaceMK3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIMachineElectricFurnaceMK3 extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_electric_furnace.png");
	private TileEntityMachineElectricFurnaceMK3 furnace;

	public GUIMachineElectricFurnaceMK3(InventoryPlayer invPlayer, TileEntityMachineElectricFurnaceMK3 furnace) {
		super(new ContainerElectricFurnaceMK3(invPlayer, furnace));
		this.furnace = furnace;
		this.xSize = 176;
		this.ySize = 186;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 52 - 34, 16, 34, furnace.power, furnace.maxPower);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = I18n.format(this.furnace.getInventoryName());
		this.fontRendererObj.drawString(name, 70 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(furnace.power > 0) {
			int p = (int) (furnace.power * 34 / furnace.maxPower);
			drawTexturedModalRect(guiLeft + 152, guiTop + 52 - p, 176, 64 - p, 16, p);
		}

		int prog = furnace.progress * 28 / furnace.maxProgress;
		drawTexturedModalRect(guiLeft + 43, guiTop + 36, 176, 0, prog, 12);
	}
}
