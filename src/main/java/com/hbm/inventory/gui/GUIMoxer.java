package com.hbm.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMoxer;
import com.hbm.inventory.recipes.MoxerRecipe;
import com.hbm.items.machine.customfuel.EnumCustomFuel;
import com.hbm.lib.RefStrings;
import com.hbm.module.machine.ModuleMachineBase;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityMoxer;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public class GUIMoxer extends GuiInfoContainerProcessor {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_moxer.png");
	private TileEntityMoxer moxer;
	protected GuiTextField field;

	// selected isotope in the list, and ticks left on the pressed look of the arrow buttons
	private int index;
	private int pressTimer;

	public GUIMoxer(InventoryPlayer invPlayer, TileEntityMoxer tedf) {
		super(new ContainerMoxer(invPlayer, tedf));
		this.moxer = tedf;

		this.processorModule = new ModuleMachineBase[1];
		this.processorModule[0] = moxer.moxerModule;

		this.xSize = 176;
		this.ySize = 256;
	}

	@Override public int[][] getSelectorPositions() { return new int[][] {{7, 125, 1}}; }
	@Override public IControlReceiver getControlReceiver() { return this.moxer; }
	@Override public ResourceLocation getTexture() { return texture; }

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		this.field = new GuiTextField(this.fontRendererObj, guiLeft + 83, guiTop + 111, 50, 14);
		this.field.setTextColor(-1);
		this.field.setDisabledTextColour(-1);
		this.field.setEnableBackgroundDrawing(false);
		this.field.setMaxStringLength(6);
	}

	private int getRecipeAmount() {
		MoxerRecipe recipe = (MoxerRecipe) moxer.moxerModule.getRecipe();
		return recipe != null ? recipe.amount : 1;
	}

	private EnumCustomFuel getSelected(List<EnumCustomFuel> isotopes) {
		if(isotopes.isEmpty()) return null;
		index = Math.max(0, Math.min(index, isotopes.size() - 1));
		return isotopes.get(index);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		List<String> info = new ArrayList();
		List<EnumCustomFuel> isotopes = moxer.getListedIsotopes();
		if(isotopes.isEmpty()) info.add(EnumChatFormatting.RED + "Empty");
		for(EnumCustomFuel fuel : isotopes) {
			info.add(EnumChatFormatting.YELLOW + String.format(Locale.US, "(%5.1f%%) %s: %d mB", (double) moxer.perCycle.get(fuel) / getRecipeAmount() * 100, I18nUtil.resolveKey(fuel.stack.getUnlocalizedName()), 2 * moxer.buffer.get(fuel)));
			if(moxer.perCycle.get(fuel) > 0) info.add(EnumChatFormatting.RED + String.format(Locale.US, "↪ -%d mB", 2 * moxer.perCycle.get(fuel)));
		}
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 62, guiTop + 8, 36, 54, mouseX, mouseY, info);

		EnumCustomFuel selected = getSelected(isotopes);
		String selectedName = selected != null ? I18nUtil.resolveKey(selected.stack.getUnlocalizedName()) : I18nUtil.resolveKey("turret.none");
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 29, guiTop + 125, 18, 18, mouseX, mouseY, new String[] {
				EnumChatFormatting.RED + I18nUtil.resolveKey("gui.moxer.void"),
				EnumChatFormatting.YELLOW + I18nUtil.resolveKey("gui.moxer.void.desc", selectedName) });

		for(int i = 0; i < moxer.inputTanks.length; i++) moxer.inputTanks[i].renderTankInfo(this, mouseX, mouseY, guiLeft + 7 + i * 18, guiTop + 8, 18, 36);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 61, moxer.power, moxer.maxPower);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);
		this.field.mouseClicked(x, y, button);

		int count = moxer.getListedIsotopes().size();

		if(count > 0 && checkClick(x, y, 8, 99, 16, 16)) { index = (index + 1) % count; click(); pressTimer = 5; }
		if(count > 0 && checkClick(x, y, 26, 99, 16, 16)) { index = (index + count - 1) % count; click(); pressTimer = 5; }

		// shift-click only, voided material is gone for good
		EnumCustomFuel selected = getSelected(moxer.getListedIsotopes());
		if(selected != null && isShiftKeyDown() && checkClick(x, y, 30, 126, 16, 16)) {
			NBTTagCompound data = new NBTTagCompound();
			data.setInteger("void", selected.ordinal());
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, moxer.xCoord, moxer.yCoord, moxer.zCoord));
			click();
		}

		if(checkClick(x, y, 44, 99, 16, 16)) {
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean("autoMode", !moxer.autoMode);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, moxer.xCoord, moxer.yCoord, moxer.zCoord));
			click();
		}
	}

	@Override
	protected void keyTyped(char c, int key) {
		if(this.field.isFocused() && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
			submitAmount();
			return;
		}
		if(this.field.textboxKeyTyped(c, key)) return;
		super.keyTyped(c, key);
	}

	// the field takes mB or a percentage of the recipe with a % suffix, the machine works in quanta (1 quantum = 2 mB)
	private void submitAmount() {
		EnumCustomFuel selected = getSelected(moxer.getListedIsotopes());
		String text = this.field.getText().trim();
		if(selected == null || text.isEmpty()) return;

		int quanta;
		try {
			if(text.endsWith("%")) {
				double percent = Double.parseDouble(text.substring(0, text.length() - 1).trim());
				quanta = (int) Math.round(percent / 100D * getRecipeAmount());
			} else {
				quanta = Integer.parseInt(text) / 2;
			}
		} catch(NumberFormatException e) { quanta = 0; }

		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("isotope", selected.ordinal());
		data.setInteger("amount", Math.max(quanta, 0));
		PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, moxer.xCoord, moxer.yCoord, moxer.zCoord));
		click();
		this.field.setText("");
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		List<EnumCustomFuel> isotopes = moxer.getListedIsotopes();
		EnumCustomFuel selected = getSelected(isotopes);

		String name = selected != null ? I18nUtil.resolveKey(selected.stack.getUnlocalizedName()) : EnumChatFormatting.ITALIC + I18nUtil.resolveKey("turret.none");
		String share = String.format(Locale.US, "%.1f%%", selected != null ? (double) moxer.perCycle.get(selected) / getRecipeAmount() * 100 : 0D);
		String total = String.format(Locale.US, "Total: %d mB", 2 * moxer.buffer.total());

		String text = this.field.getText();
		if(this.field.isFocused()) {
			String cursor = System.currentTimeMillis() % 1000 < 500 ? " " : "||";
			text = text.substring(0, this.field.getCursorPosition()) + cursor + text.substring(this.field.getCursorPosition());
		}

		double scale = 2;
		GL11.glScaled(1D / scale, 1D / scale, 1);
		this.fontRendererObj.drawString(total, (int) (83 * scale), (int) (77 * scale), 0x00ff00);
		this.fontRendererObj.drawString(name, (int) (83 * scale), (int) (83 * scale), 0x00ff00);
		this.fontRendererObj.drawString(share, (int) (83 * scale), (int) (89 * scale), 0x00ff00);
		this.fontRendererObj.drawString("Status: " + moxer.status.name(), (int) (83 * scale), (int) (95 * scale), 0x00ff00);
		this.fontRendererObj.drawString(text, (int) (83 * scale), (int) (116 * scale), 0x00ff00);
		GL11.glScaled(scale, scale, 1);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		drawBuffer(62, 61);

		// arrow buttons light up on hover and look pressed right after a click
		if(isMouseOverSlot(mouseX, mouseY, 7, 98)) drawTexturedModalRect(guiLeft + 7, guiTop + 98, 176, pressTimer > 0 ? 147 : 129, 18, 18);
		if(isMouseOverSlot(mouseX, mouseY, 25, 98)) drawTexturedModalRect(guiLeft + 25, guiTop + 98, 194, pressTimer > 0 ? 147 : 129, 18, 18);
		if(isMouseOverSlot(mouseX, mouseY, 43, 98)) drawTexturedModalRect(guiLeft + 43, guiTop + 98, 212, 129, 18, 18);
		if(isMouseOverSlot(mouseX, mouseY, 29, 125)) drawTexturedModalRect(guiLeft + 29, guiTop + 125, 230, 129, 18, 18);
		if(moxer.autoMode) drawTexturedModalRect(guiLeft + 43, guiTop + 98, 212, 147, 18, 18);
		if(pressTimer > 0) pressTimer--;

		if(moxer.moxerModule.progress > 0) {
			int j = (int) Math.ceil(70 * moxer.moxerModule.progress);
			drawTexturedModalRect(guiLeft + 62, guiTop + 126, 176, 61, j, 16);
		}

		int p = (int) (moxer.power * 61 / moxer.maxPower);
		drawTexturedModalRect(guiLeft + 152, guiTop + 79 - p, 176, 61 - p, 16, p);

		this.renderStandardLEDs(moxer.didProcess, moxer.moxerModule.getRecipe(), moxer.power, 51, 121, 195, 0);
		this.renderRecipeIcons();

		for(int i = 0; i < moxer.inputTanks.length; i++) moxer.inputTanks[i].renderTank(guiLeft + 8 + i * 18, guiTop + 43, this.zLevel, 16, 34);
	}

	private boolean isMouseOverSlot(int mouseX, int mouseY, int x, int y) {
		return guiLeft + x <= mouseX && guiLeft + x + 18 > mouseX && guiTop + y < mouseY && guiTop + y + 18 >= mouseY;
	}

	// stacked bars of the buffer contents in their molten colours, like the crucible
	private void drawBuffer(int x, int y) {
		int lastHeight = 0;
		int lastQuant = 0;

		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			int amount = moxer.buffer.get(fuel);
			if(amount <= 0) continue;

			int targetHeight = (lastQuant + amount) * 52 / TileEntityMoxer.bufferCapacity;
			lastQuant += amount;
			if(lastHeight == targetHeight) continue;

			int color = fuel.stack.moltenColor;
			GL11.glColor3f(((color >> 16) & 0xff) / 255F, ((color >> 8) & 0xff) / 255F, (color & 0xff) / 255F);
			drawTexturedModalRect(guiLeft + x, guiTop + y - targetHeight, 176, 129 - targetHeight, 34, targetHeight - lastHeight);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			GL11.glColor4f(1F, 1F, 1F, 0.3F);
			drawTexturedModalRect(guiLeft + x, guiTop + y - targetHeight, 176, 129 - targetHeight, 34, targetHeight - lastHeight);
			GL11.glDisable(GL11.GL_BLEND);

			lastHeight = targetHeight;
		}

		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor4f(1F, 1F, 1F, 1F);
	}
}
