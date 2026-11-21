package com.hbm.items.machine;

import java.util.List;
import java.util.Locale;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelModel;
import com.hbm.items.machine.customfuel.CustomFuelModel.PWRProfile;
import com.hbm.items.machine.customfuel.CustomFuelModel.WatzProfile;
import com.hbm.items.machine.customfuel.CustomFuelModel.ZirnoxProfile;
import com.hbm.items.machine.customfuel.CustomFuelSpent;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

public class ItemCustomFuel extends Item implements ICustomFuelItem {

	public static enum Form {
		INGOT(72, false),
		BILLET(48, false),
		PWR(96, false),
		PWR_HOT(96, true),
		PWR_DEPLETED(96, true),
		ZIRNOX(96, false),
		ZIRNOX_DEPLETED(96, true),
		WATZ(288, false),
		WATZ_DEPLETED(288, true);

		public final int amount;
		public final boolean spent;

		private Form(int amount, boolean spent) {
			this.amount = amount;
			this.spent = spent;
		}
	}

	public static final double WATZ_YIELD = 1_000_000_000D;

	public final Form form;
	protected IIcon iconBG;

	public ItemCustomFuel(Form form) {
		this.form = form;
		this.setMaxStackSize(form == Form.ZIRNOX || form == Form.ZIRNOX_DEPLETED ? 1 : 64);
	}

	@Override
	public int getFuelAmount() {
		return form.amount;
	}

	public static ItemStack setup(ItemStack stack, List<MaterialStack> mats, int amount) {
		CustomFuelMix mix = new CustomFuelMix(mats);
		mix.fresh = amount;
		return mix.writeTo(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		if(form.spent) return;
		for(int[] preset : ICustomFuelItem.PRESETS) list.add(ICustomFuelItem.makePreset(this, preset));
	}


	public static int getLife(ItemStack stack) {
		return stack.hasTagCompound() ? stack.stackTagCompound.getInteger("life") : 0;
	}

	public static void setLife(ItemStack stack, int life) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setInteger("life", life);
	}


	public static double getWatzYield(ItemStack stack) {
		if(!stack.hasTagCompound() || !stack.stackTagCompound.hasKey("yield")) return WATZ_YIELD;
		return stack.stackTagCompound.getDouble("yield");
	}

	public static void setWatzYield(ItemStack stack, double yield) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setDouble("yield", yield);
	}

	public static double getWatzDepletion(ItemStack stack) {
		return 1D - getWatzYield(stack) / WATZ_YIELD;
	}

	public static double getZirnoxDepletion(ItemStack stack) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) return 0D;
		ZirnoxProfile p = CustomFuelModel.zirnox(mix, 0D);
		return Math.min((double) getLife(stack) / p.maxLife, 1D);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return getDurabilityForDisplay(stack) > 0D;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		if(form == Form.ZIRNOX) return getZirnoxDepletion(stack);
		if(form == Form.WATZ) return getWatzDepletion(stack);
		return 0D;
	}


	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		return pass == 1 ? super.getIconFromDamageForRenderPass(meta, pass) : this.iconBG;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if(pass != 0) return 0xffffff;
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) return 0xffffff;
		int color = mix.getColor();
		return form.spent ? ICustomFuelItem.darken(color) : color;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		this.iconBG = reg.registerIcon(this.iconString + "_bg");
	}


	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) {
			list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.noMix"));
			return;
		}

		ICustomFuelItem.addComposition(mix, list);

		String model = CustomFuelModel.current().title;
		switch(form) {
		case PWR: {
			PWRProfile p = CustomFuelModel.pwr(mix, 0D);
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.customFuel.model", model));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.reaction", EnumChatFormatting.WHITE + p.describe()));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.heat", String.format(Locale.US, "%.2f", p.heat)));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.yield", BobMathUtil.getShortNumber((long) p.yield)));
			break;
		}
		case ZIRNOX: {
			ZirnoxProfile p = CustomFuelModel.zirnox(mix, getZirnoxDepletion(stack));
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.customFuel.model", model));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.depletion", String.format(Locale.US, "%.1f%%", getZirnoxDepletion(stack) * 100D)));
			if(p.breeding) list.add(EnumChatFormatting.DARK_GREEN + I18nUtil.resolveKey("desc.item.customFuel.zirnoxBreeder", BobMathUtil.getShortNumber(p.maxLife)));
			else list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.zirnox", p.heat, BobMathUtil.getShortNumber(p.maxLife)));
			break;
		}
		case WATZ: {
			WatzProfile p = CustomFuelModel.watz(mix, getWatzDepletion(stack));
			list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.customFuel.model", model));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.rbmk.depletion", String.format(Locale.US, "%.1f%%", getWatzDepletion(stack) * 100D)));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.reaction", EnumChatFormatting.WHITE + p.describe()));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.heat", String.format(Locale.US, "%.2f", p.heatEmission)));
			if(p.passive > 0) list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.passive", (int) p.passive));
			break;
		}
		default: break;
		}

		if(form == Form.PWR_HOT) list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.hot"));
		if(form.spent && form != Form.PWR_HOT) ICustomFuelItem.addSpentInfo(stack, CustomFuelSpent.reactorOf(this), mix, list);
	}
}
