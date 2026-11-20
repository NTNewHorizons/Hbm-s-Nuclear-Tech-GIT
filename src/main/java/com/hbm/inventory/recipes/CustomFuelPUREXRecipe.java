package com.hbm.inventory.recipes;

import java.util.List;

import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelSpent;

import net.minecraft.item.ItemStack;

// PUREX recipe whose outputs are worked out from the spent fuel's actual mix, the listed outputs are only for display
public class CustomFuelPUREXRecipe extends PUREXRecipe {

	public static final String PREFIX = "purex.customfuel.";

	public CustomFuelPUREXRecipe(String name) {
		super(name);
	}

	public static List<ItemStack> getOutputs(ItemStack input) {
		CustomFuelMix mix = CustomFuelMix.fromStack(input);
		return mix == null ? null : CustomFuelSpent.purexOutputs(mix);
	}
}
