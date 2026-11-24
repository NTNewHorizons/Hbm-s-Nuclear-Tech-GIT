package com.hbm.items.machine;

import java.util.List;
import java.util.Locale;

import com.hbm.config.MachineConfig;
import com.hbm.items.ModItems;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelSpent;
import com.hbm.items.machine.customfuel.CustomFuelSpent.Reactor;
import com.hbm.items.machine.customfuel.EnumCustomFuel;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

public interface ICustomFuelItem {

	public int getFuelAmount();

	// isotope ordinal / percent pairs
	public static final int[][] PRESETS = new int[][] {
		{ EnumCustomFuel.U235.ordinal(), 5, EnumCustomFuel.U238.ordinal(), 95 },
		{ EnumCustomFuel.U235.ordinal(), 90, EnumCustomFuel.U238.ordinal(), 10 },
		{ EnumCustomFuel.PU239.ordinal(), 8, EnumCustomFuel.U238.ordinal(), 92 },
		{ EnumCustomFuel.U233.ordinal(), 10, EnumCustomFuel.TH232.ordinal(), 90 },
		{ EnumCustomFuel.PU239.ordinal(), 20, EnumCustomFuel.NP237.ordinal(), 10, EnumCustomFuel.U238.ordinal(), 70 },
		{ EnumCustomFuel.SA326.ordinal(), 30, EnumCustomFuel.U235.ordinal(), 20, EnumCustomFuel.U238.ordinal(), 50 }
	};

	public static ItemStack makePreset(Item item, int[] preset) {
		int amount = ((ICustomFuelItem) item).getFuelAmount();
		CustomFuelMix mix = new CustomFuelMix();
		for(int i = 0; i < preset.length / 2; i++) mix.amounts[preset[i * 2]] = amount * preset[i * 2 + 1] / 100;
		mix.fresh = amount;
		return mix.writeTo(new ItemStack(item));
	}

	public static final String KEY_SAMPLE = "moxerSample";

	// NEI only: no mix, so it can't be crafted, burned or dissolved, the tooltip explains the Moxer instead
	public static ItemStack makeSample(Item item) {
		ItemStack stack = new ItemStack(item);
		stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setBoolean(KEY_SAMPLE, true);
		return stack;
	}

	public static boolean isSample(ItemStack stack) {
		return stack != null && stack.hasTagCompound() && stack.stackTagCompound.getBoolean(KEY_SAMPLE);
	}

	public static void addSampleInfo(ItemStack stack, List list) {
		int mB = ((ICustomFuelItem) stack.getItem()).getFuelAmount() * 2;
		list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("desc.item.customFuel.sample.title"));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.sample.input"));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.sample.mixed"));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.sample.amounts", mB));
		list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("desc.item.customFuel.sample.start"));
		list.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(stack.getItem() == ModItems.custom_fuel_ingot ? "desc.item.customFuel.sample.ingot" : "desc.item.customFuel.sample.billet"));
		list.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.item.customFuel.sample.sources"));
		list.add(EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.ITALIC + I18nUtil.resolveKey("desc.item.customFuel.sample.note"));
	}

	// The mix as it is now, partly used fuel in a reactor slot already counts as burned
	public static CustomFuelMix currentMix(ItemStack stack) {
		CustomFuelMix mix = CustomFuelMix.fromStack(stack);
		if(mix == null) return null;
		Item item = stack.getItem();
		double depletion = 0D;
		if(item instanceof ItemCustomRBMKRod && stack.stackTagCompound.hasKey("yield")) depletion = ItemCustomRBMKRod.getDepletion(stack);
		if(item == ModItems.custom_fuel_zirnox) depletion = ItemCustomFuel.getZirnoxDepletion(stack);
		if(item == ModItems.custom_fuel_watz) depletion = ItemCustomFuel.getWatzDepletion(stack);
		return depletion > 0 ? mix.burnUp(depletion) : mix;
	}

	public static int darken(int color) {
		int r = (color & 0xff0000) >> 16;
		int g = (color & 0x00ff00) >> 8;
		int b = color & 0x0000ff;
		int avg = (r + g + b) / 3;
		r = (int) ((r - (r - avg) * 0.7) * 0.6);
		g = (int) ((g - (g - avg) * 0.7) * 0.6);
		b = (int) ((b - (b - avg) * 0.7) * 0.6);
		return r << 16 | g << 8 | b;
	}

	public static void addComposition(CustomFuelMix mix, List list) {
		int fresh = Math.max(mix.fresh, 1);
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			int amount = mix.get(fuel);
			if(amount <= 0) continue;
			list.add(EnumChatFormatting.YELLOW + String.format(Locale.US, "(%5.1f%%) %s: %d mB", (double) amount / fresh * 100D, I18nUtil.resolveKey(fuel.stack.getUnlocalizedName()), amount * 2));
		}
		int products = mix.fresh - mix.total();
		if(products > 0) {
			list.add(EnumChatFormatting.DARK_GRAY + String.format(Locale.US, "(%5.1f%%) %s: %d mB", (double) products / fresh * 100D, I18nUtil.resolveKey("desc.item.customFuel.products"), products * 2));
		}
		list.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("desc.item.customFuel.fissile", String.format(Locale.US, "%.1f%%", mix.fissileShare() * 100D)));
	}

	public static void addSpentInfo(ItemStack stack, Reactor reactor, CustomFuelMix mix, List list) {
		if(MachineConfig.customFuelRecycles()) list.add(EnumChatFormatting.AQUA + I18nUtil.resolveKey("desc.item.customFuel.recycle"));
		if(MachineConfig.customFuelPUREX()) list.add(EnumChatFormatting.AQUA + I18nUtil.resolveKey("desc.item.customFuel.purex"));
		if(MachineConfig.customFuelVanillaCraft() && reactor != null) {
			ItemStack vanilla = CustomFuelSpent.closestVanilla(reactor, mix, stack.getItemDamage() % 5);
			if(vanilla != null) list.add(EnumChatFormatting.AQUA + I18nUtil.resolveKey("desc.item.customFuel.vanilla", vanilla.getDisplayName()));
		}
	}
}
