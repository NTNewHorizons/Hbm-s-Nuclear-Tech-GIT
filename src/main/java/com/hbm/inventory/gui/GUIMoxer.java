package com.hbm.inventory.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.hbm.inventory.recipes.MoxerRecipe;
import com.hbm.inventory.recipes.MoxerRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMoxer;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MoxerStack;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMoxer;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public class GUIMoxer extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_moxer.png");
	private TileEntityMoxer moxer;
	protected GuiTextField field;

	int index;
	int timer;
	boolean return_prs;
	boolean return_old;

	public void initGui() {

		super.initGui();

		Keyboard.enableRepeatEvents(true);
		this.field = new GuiTextField(this.fontRendererObj, guiLeft + 83, guiTop + 111, 50, 14);
		this.field.setTextColor(-1);
		this.field.setDisabledTextColour(-1);
		this.field.setEnableBackgroundDrawing(false);
		this.field.setMaxStringLength(6);
	}

	public GUIMoxer(InventoryPlayer invPlayer, TileEntityMoxer tedf) {
		super(new ContainerMoxer(invPlayer, tedf));
		moxer = tedf;

		this.xSize = 176;
		this.ySize = 256;
	}

	@Override
	public void drawScreen(int x, int y, float interp) {
		super.drawScreen(x, y, interp);

		drawStackInfo(moxer.inpStack, x, y, 62, 8);

		for (int i = 0; i < moxer.inputTanks.length; i++)
			moxer.inputTanks[i].renderTankInfo(this, x, y, guiLeft + 7 + i * 18, guiTop + 8, 18, 36);

		this.drawElectricityInfo(this, x, y, guiLeft + 152, guiTop + 18, 16, 61, moxer.power, moxer.maxPower);

		if(guiLeft + 7 <= x && guiLeft + 7 + 18 > x && guiTop + 125 < y && guiTop + 125 + 18 >= y) {
			if(this.moxer.moxerModule.getRecipeName() != null && MoxerRecipes.INSTANCE.recipeNameMap.containsKey(this.moxer.moxerModule.getRecipeName())) {
				MoxerRecipe recipe = (MoxerRecipe) MoxerRecipes.INSTANCE.recipeNameMap.get(this.moxer.moxerModule.getRecipeName());
				GUIElements.drawHoveringTextRecipe(recipe.print(), x, y, this.fontRendererObj, itemRender, this.width, this.height);
			} else {
				this.drawCreativeTabHoveringText(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("gui.recipe.setRecipe"), x, y);
			}
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);

		boolean flag = x >= this.field.xPosition && x < this.field.xPosition + this.field.width && y >= this.field.yPosition && y < this.field.yPosition + this.field.height;
		this.field.setFocused(flag);

		int count = getCount();

		if(count > 0) {

			// selector down
			if(guiLeft + 8 <= x && guiLeft + 8 + 16 > x && guiTop + 99 < y && guiTop + 99 + 16 >= y) {

				index++;
				index %= count;
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				timer = 5;
				return;
			}
			// selector up
			if(guiLeft + 26 <= x && guiLeft + 26 + 16 > x && guiTop + 99 < y && guiTop + 99 + 16 >= y) {

				index--;
				if(index < 0)
					index = count - 1;
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				timer = 5;
				return;
			}
			// shitter
			if(guiLeft + 44 <= x && guiLeft + 44 + 16 > x && guiTop + 99 < y && guiTop + 99 + 16 >= y) {

				moxer.autoMode = !moxer.autoMode;
				NBTTagCompound data = new NBTTagCompound();
				data.setBoolean("autoChange", moxer.autoMode);
				PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, moxer.xCoord, moxer.yCoord, moxer.zCoord));
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				return;
			}
		}

		if(this.checkClick(x, y, 7, 125, 18, 18))
			GUIScreenRecipeSelector.openSelector(MoxerRecipes.INSTANCE, moxer, moxer.moxerModule.getRecipeName(), 0, ItemBlueprints.grabPool(moxer.slots[1]), this);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {

		return_prs = Keyboard.isKeyDown(Keyboard.KEY_RETURN);
		MoxerRecipe recipe = MoxerRecipes.INSTANCE.recipeNameMap.get(moxer.moxerModule.getRecipeName());
		int recAmount = 1;
		if (recipe != null)
			recAmount = recipe.amount;

		if (return_prs && !return_old && recipe != null) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));

			if(this.field.getText().isEmpty())
				return;

			NBTTagCompound data = new NBTTagCompound();
			int percentage;
			try {
				percentage = Integer.parseInt(this.field.getText());
			} catch (NumberFormatException e) {
				percentage = 0;
			}
			percentage = MathHelper.clamp_int(percentage, 0, 2 * recAmount);
			percentage /= 2; // because 1 quantum = 2 mB, ffs
			data.setInteger("perc_index", index);
			data.setInteger("percentage", percentage);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, moxer.xCoord, moxer.yCoord, moxer.zCoord));

			this.field.setText("");
		}
		return_old = return_prs;

		List<String> names = moxer.getStringMaterials();

		String n = EnumChatFormatting.ITALIC + I18nUtil.resolveKey("turret.none");

		if(this.index >= this.getCount())
			this.index = this.getCount()-1;

		if(index < 0)
			index = 0;

		if(names != null) {
			n = I18nUtil.resolveKey(names.get(index));
		}

		String t = this.field.getText();
		String p = String.format("%.1f%s", moxer.inpStack.isEmpty() ? 0 : (double) moxer.inpStack.get(index).percentage / recAmount * 100, "%");

		int amount = 0;
		for (MoxerStack mat : moxer.inpStack)
			amount += mat.amount;
		String total = String.format("Total: %d mB", 2 * amount);

		String cursor = System.currentTimeMillis() % 1000 < 500 ? " " : "||";

		if(this.field.isFocused())
			t = t.substring(0, this.field.getCursorPosition()) + cursor + t.substring(this.field.getCursorPosition(), t.length());

		double scale = 2;

		GL11.glScaled(1D / scale, 1D / scale, 1);
		this.fontRendererObj.drawString(total, (int)(83 * scale), (int)(77 * scale), 0x00ff00);
		this.fontRendererObj.drawString(n, (int)(83 * scale), (int)(83 * scale), 0x00ff00);
		this.fontRendererObj.drawString(p, (int)(83 * scale), (int)(89 * scale), 0x00ff00);
		this.fontRendererObj.drawString("Status: " + moxer.getStatus(moxer.status), (int)(83 * scale), (int)(95 * scale), 0x00ff00);
		this.fontRendererObj.drawString(t, (int)(83 * scale), (int)(116 * scale), 0x00ff00);
		GL11.glScaled(scale, scale, 1);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		if(!moxer.inpStack.isEmpty()) drawStack(moxer.inpStack, moxer.inpZCapacity, 62, 61);

		// selector down
		if(guiLeft + 7 <= p_146976_2_ && guiLeft + 7 + 18 > p_146976_2_ && guiTop + 98 < p_146976_3_ && guiTop + 98 + 18 >=p_146976_3_) {
			if (timer > 0)
				drawTexturedModalRect(guiLeft + 7, guiTop + 98, 176, 147, 18, 18);
			else
				drawTexturedModalRect(guiLeft + 7, guiTop + 98, 176, 129, 18, 18);
		}
		// selector up
		if(guiLeft + 25 <= p_146976_2_ && guiLeft + 25 + 18 > p_146976_2_ && guiTop + 98 < p_146976_3_ && guiTop + 98 + 18 >=p_146976_3_) {
			if (timer > 0)
				drawTexturedModalRect(guiLeft + 25, guiTop + 98, 194, 147, 18, 18);
			else
				drawTexturedModalRect(guiLeft + 25, guiTop + 98, 194, 129, 18, 18);
		}
		// shitter
		if(guiLeft + 43 <= p_146976_2_ && guiLeft + 43 + 18 > p_146976_2_ && guiTop + 98 < p_146976_3_ && guiTop + 98 + 18 >=p_146976_3_) {
			drawTexturedModalRect(guiLeft + 43, guiTop + 98, 212, 129, 18, 18);
		}
		if (moxer.autoMode)
			drawTexturedModalRect(guiLeft + 43, guiTop + 98, 212, 147, 18, 18);

		if (timer > 0)
			timer--;

		// progress bar
		drawTexturedModalRect(guiLeft + 62, guiTop + 126, 176, 61, (int) (70 * moxer.progress), 16);

		// power
		int p = (int) (moxer.power * 61 / moxer.maxPower);
		drawTexturedModalRect(guiLeft + 152, guiTop + 79 - p, 176, 61 - p, 16, p);

		for (int i = 0; i < moxer.inputTanks.length; i++)
			moxer.inputTanks[i].renderTank(guiLeft + 8 + i * 18, guiTop + 43, this.zLevel, 16, 34);

		MoxerRecipe recipe = MoxerRecipes.INSTANCE.recipeNameMap.get(moxer.moxerModule.getRecipeName());

		/// LEFT LED
		if(moxer.progress > 0) {
			drawTexturedModalRect(guiLeft + 51, guiTop + 121, 195, 0, 3, 6);
		} else if(recipe != null) {
			drawTexturedModalRect(guiLeft + 51, guiTop + 121, 192, 0, 3, 6);
		}

		/// RIGHT LED
		if(moxer.progress > 0) {
			drawTexturedModalRect(guiLeft + 56, guiTop + 121, 195, 0, 3, 6);
		} else if(recipe != null && moxer.power >= recipe.power) {
			drawTexturedModalRect(guiLeft + 56, guiTop + 121, 192, 0, 3, 6);
		}

		this.renderItem(recipe != null ? recipe.getIcon() : TEMPLATE_FOLDER, 8, 126);

		if(recipe != null && recipe.inputItem != null) {
			for(int i = 0; i < recipe.inputItem.length; i++) {
				Slot slot = (Slot) this.inventorySlots.inventorySlots.get(moxer.moxerModule.inputSlots[i]);
				if(!slot.getHasStack()) this.renderItem(recipe.inputItem[i].extractForCyclingDisplay(20), slot.xDisplayPosition, slot.yDisplayPosition, 10F);
			}

			Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
			OpenGlHelper.glBlendFunc(770, 771, 1, 0);
			GL11.glColor4f(1F, 1F, 1F, 0.5F);
			GL11.glEnable(GL11.GL_BLEND);
			this.zLevel = 300F;
			for(int i = 0; i < recipe.inputItem.length; i++) {
				Slot slot = (Slot) this.inventorySlots.inventorySlots.get(moxer.moxerModule.inputSlots[i]);
				if(!slot.getHasStack()) drawTexturedModalRect(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition, slot.xDisplayPosition, slot.yDisplayPosition, 16, 16);
			}
			this.zLevel = 0F;
			GL11.glColor4f(1F, 1F, 1F, 1F);
			GL11.glDisable(GL11.GL_BLEND);
		}
	}

	protected void drawStackInfo(List<MoxerStack> stack, int mouseX, int mouseY, int x, int y) {

		List<String> list = new ArrayList();

		if(stack.isEmpty())
			list.add(EnumChatFormatting.RED + "Empty");

		int recAmount = 1;
		MoxerRecipe recipe = MoxerRecipes.INSTANCE.recipeNameMap.get(moxer.moxerModule.getRecipeName());
		if (recipe != null)
			recAmount = recipe.amount;
		for(MoxerStack sta : stack) {
			list.add(EnumChatFormatting.YELLOW + String.format("(%5.1f%s) %s: %d mB", (double) sta.percentage / recAmount * 100, "%", I18nUtil.resolveKey(sta.material.getUnlocalizedName()), 2 * sta.amount));
			if (sta.percentage != 0)
				list.add(EnumChatFormatting.RED + String.format("↪ -%d mB", 2 * sta.percentage));
		}

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + x, guiTop + y, 36, 54, mouseX, mouseY, list);
	}

	protected void drawStack(List<MoxerStack> stack, int capacity, int x, int y) {

		if(stack.isEmpty()) return;

		int lastHeight = 0;
		int lastQuant = 0;

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		for(MoxerStack sta : stack) {

			int targetHeight = (lastQuant + sta.amount) * 52 / capacity;

			if(lastHeight == targetHeight) continue; //skip draw calls that would be 0 pixels high

			int offset = sta.material.smeltable == SmeltingBehavior.ADDITIVE ? 34 : 0; //additives use a differnt texture

			int hex = sta.material.moltenColor;
			//hex = 0xC18336;
			Color color = new Color(hex);
			GL11.glColor3f(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F);
			drawTexturedModalRect(guiLeft + x, guiTop + y - targetHeight, 176 + offset, 129 - targetHeight, 34, targetHeight - lastHeight);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glColor4f(1F, 1F, 1F, 0.3F);
			drawTexturedModalRect(guiLeft + x, guiTop + y - targetHeight, 176 + offset, 129 - targetHeight, 34, targetHeight - lastHeight);
			GL11.glDisable(GL11.GL_BLEND);

			lastQuant += sta.amount;
			lastHeight = targetHeight;
		}

		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor3f(255, 255, 255);
	}

	private int getCount() {

		List<Mats.MoxerStack> materials = moxer.inpStack;

		if(materials == null)
			return 0;

		return materials.size();
	}

	protected void keyTyped(char c, int i) {
		if(this.field.textboxKeyTyped(c, i)) return;
		super.keyTyped(c, i);
	}
}
