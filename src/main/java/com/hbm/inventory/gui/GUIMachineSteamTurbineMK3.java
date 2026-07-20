package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerSteamTurbineMK3;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineSteamTurbineMK3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIMachineSteamTurbineMK3 extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_turbine.png");
	private TileEntityMachineSteamTurbineMK3 turbine;

	public GUIMachineSteamTurbineMK3(InventoryPlayer invPlayer, TileEntityMachineSteamTurbineMK3 turbine) {
		super(new ContainerSteamTurbineMK3(invPlayer, turbine));
		this.turbine = turbine;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 69 - 52, 16, 52, turbine.power, turbine.maxPower);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = I18n.format(this.turbine.getInventoryName());
		this.fontRendererObj.drawString(name, 70 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(turbine.power > 0) {
			int p = (int) (turbine.power * 52 / turbine.maxPower);
			drawTexturedModalRect(guiLeft + 152, guiTop + 69 - p, 176, 52 - p, 16, p);
		}
	}
}
