package com.hbm.inventory.gui;

import java.util.Locale;

import api.hbm.energymk2.IEnergyHandlerMK2;
import cofh.api.energy.EnergyStorage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.TileEntityConverterHeRf;
import com.hbm.tileentity.network.TileEntityConverterRfHe;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;

@SideOnly(Side.CLIENT)
public class GUIEnergyConverter extends GuiScreen {

	private final TileEntityLoadedBase tile;
	private final IEnergyHandlerMK2 energy;
	private final EnergyStorage storage;
	private final boolean heToRf;
	private int left;
	private int top;
	private GuiButton decrease;
	private GuiButton increase;
	private GuiButton priority;

	public GUIEnergyConverter(TileEntityConverterHeRf converter) {
		tile = converter;
		energy = converter;
		storage = converter.storage;
		heToRf = true;
	}

	public GUIEnergyConverter(TileEntityConverterRfHe converter) {
		tile = converter;
		energy = converter;
		storage = converter.storage;
		heToRf = false;
	}

	@Override
	public void initGui() {
		left = (width - (heToRf ? 280 : 170)) / 2;
		top = (height - 32) / 2;
		buttonList.clear();
		buttonList.add(decrease = new GuiButton(0, left, top + 12, 20, 20, "-"));
		buttonList.add(increase = new GuiButton(1, left + 150, top + 12, 20, 20, "+"));
		if(heToRf) buttonList.add(priority = new GuiButton(2, left + 190, top + 12, 90, 20, ""));
	}

	@Override
	public void updateScreen() {
		if(tile.isInvalid() || mc.theWorld.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile
				|| mc.thePlayer.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) > 64) {
			mc.thePlayer.closeScreen();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, I18nUtil.resolveKey("gui.converter.capacity"), left + 85, top, 0xA0A0A0);
		drawCenteredString(fontRendererObj, BobMathUtil.getShortNumber(energy.getMaxPower()) + " HE / "
				+ BobMathUtil.getShortNumber(storage.getMaxEnergyStored()) + " RF", left + 85, top + 18, 0xFFFFFF);
		decrease.enabled = energy.getMaxPower() > 100_000;
		increase.enabled = energy.getMaxPower() < 5_000_000;
		if(heToRf) {
			drawCenteredString(fontRendererObj, I18nUtil.resolveKey("gui.converter.priority"), left + 235, top, 0xA0A0A0);
			priority.displayString = I18nUtil.resolveKey("gui.connection_priority."
					+ ((TileEntityConverterHeRf) tile).getPriority().name().toLowerCase(Locale.ROOT));
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		NBTTagCompound data = new NBTTagCompound();
		if(button.id == 0 || button.id == 1) data.setInteger("bufferStep", button.id == 0 ? -1 : 1);
		else if(heToRf && button.id == 2) {
			data.setInteger("priorityStep", 1);
		} else return;
		PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, tile.xCoord, tile.yCoord, tile.zCoord));
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) {
		if(keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) mc.thePlayer.closeScreen();
		else super.keyTyped(typedChar, keyCode);
	}

	@Override public boolean doesGuiPauseGame() { return false; }
}
