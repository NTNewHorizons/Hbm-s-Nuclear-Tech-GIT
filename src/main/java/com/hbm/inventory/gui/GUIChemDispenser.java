package com.hbm.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.chemistry.ChemReagent;
import com.hbm.inventory.chemistry.ChemReagents;
import com.hbm.inventory.chemistry.ReagentHolder.Entry;
import com.hbm.inventory.container.ContainerChemDispenser;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.AuxButtonPacket;
import com.hbm.tileentity.machine.TileEntityMachineChemDispenser;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

public class GUIChemDispenser extends GuiInfoContainer {

	private final TileEntityMachineChemDispenser dispenser;

	public GUIChemDispenser(TileEntityMachineChemDispenser dispenser) {
		super(new ContainerChemDispenser(dispenser));
		this.dispenser = dispenser;
		this.xSize = 248;
		this.ySize = 220;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		super.initGui();
		buttonList.clear();
		int index = 0;
		for(ChemReagent reagent : ChemReagents.dispensable()) {
			int column = index % 2;
			int row = index / 2;
			buttonList.add(new GuiButton(index, guiLeft + 9 + column * 70, guiTop + 30 + row * 22, 66, 20, I18n.format(reagent.translationKey)));
			index++;
		}
		buttonList.add(new GuiButton(100, guiLeft + 9, guiTop + 127, 38, 20, "1u"));
		buttonList.add(new GuiButton(101, guiLeft + 50, guiTop + 127, 38, 20, "5u"));
		buttonList.add(new GuiButton(102, guiLeft + 91, guiTop + 127, 38, 20, "10u"));
		buttonList.add(new GuiButton(110, guiLeft + 9, guiTop + 153, 120, 20, I18n.format("gui.chem.ss13.flush")));
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if(button.id >= 0 && button.id < ChemReagents.dispensable().size()) send(button.id, 0);
		if(button.id >= 100 && button.id <= 102) send(button.id - 100, 1);
		if(button.id == 110) send(0, 2);
	}

	private void send(int value, int action) {
		PacketDispatcher.wrapper.sendToServer(new AuxButtonPacket(dispenser.xCoord, dispenser.yCoord, dispenser.zCoord, value, action));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1, 1, 1, 1);
		drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF182129);
		drawRect(guiLeft + 5, guiTop + 20, guiLeft + 135, guiTop + 178, 0xFF26343E);
		drawRect(guiLeft + 140, guiTop + 20, guiLeft + 243, guiTop + 178, 0xFF202B33);
		drawRect(guiLeft + 5, guiTop + 183, guiLeft + 243, guiTop + 215, 0xFF10171C);

		int selected = dispenser.getDispenseAmount() == 1 ? 0 : dispenser.getDispenseAmount() == 5 ? 1 : 2;
		drawRect(guiLeft + 8 + selected * 41, guiTop + 126, guiLeft + 48 + selected * 41, guiTop + 148, 0xFF63B6C8);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRendererObj.drawString(I18n.format("container.machineChemDispenser"), 8, 7, 0xE8F4F8);
		fontRendererObj.drawString(I18n.format("gui.chem.ss13.contents"), 146, 26, 0xA6DCE6);

		int y = 40;
		for(Entry entry : dispenser.holder.getContents()) {
			if(y > 130) break;
			String name = I18n.format(entry.reagent.translationKey);
			fontRendererObj.drawString(trim(name, 68), 146, y, entry.reagent.color);
			fontRendererObj.drawString(String.format("%.1fu  %.0f%%", entry.amount, entry.purity * 100), 146, y + 9, 0xC5D0D5);
			y += 18;
		}
		if(dispenser.holder.getContents().isEmpty()) fontRendererObj.drawString(I18n.format("gui.chem.ss13.empty"), 146, y, 0x71818A);

		fontRendererObj.drawString(String.format("%.1f / %.0fu", dispenser.holder.getTotalAmount(), dispenser.holder.getCapacity()), 146, 150, 0xE3E8EA);
		fontRendererObj.drawString(String.format("%.1fK  pH %.2f", dispenser.holder.getTemperature(), dispenser.holder.getPH()), 146, 160, 0xE3E8EA);

		String power = String.format("HE: %,d / %,d", dispenser.getPower(), dispenser.getMaxPower());
		fontRendererObj.drawString(power, 10, 190, 0x79D6EA);
		String reaction = dispenser.getActiveReaction().isEmpty() ? I18n.format("gui.chem.ss13.idle") : I18n.format(dispenser.getActiveReaction());
		fontRendererObj.drawString(trim(reaction, 142), 10, 202, 0xD5E2E6);
		if(dispenser.isShowingInverseNotice()) fontRendererObj.drawString(EnumChatFormatting.RED + I18n.format("gui.chem.ss13.inverse"), 145, 190, 0xFF5555);
	}

	private String trim(String text, int width) {
		return fontRendererObj.trimStringToWidth(text, width);
	}
}
