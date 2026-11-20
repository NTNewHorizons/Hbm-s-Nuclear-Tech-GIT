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
