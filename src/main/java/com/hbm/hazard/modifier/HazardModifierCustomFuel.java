package com.hbm.hazard.modifier;

import com.hbm.items.machine.ICustomFuelItem;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.EnumCustomFuel;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

// Radiation and blinding of custom fuel come from the mix on the stack, the entry's base level is ignored
public class HazardModifierCustomFuel extends HazardModifier {

	private final boolean blinding;

	public HazardModifierCustomFuel(boolean blinding) {
		this.blinding = blinding;
	}

	@Override
	public float modify(ItemStack stack, EntityLivingBase holder, float level) {
		CustomFuelMix mix = ICustomFuelItem.currentMix(stack);
		if(mix == null) return 0F;
		if(blinding) return (mix.get(EnumCustomFuel.SA326) + mix.get(EnumCustomFuel.SA327)) / 72F * 5F;
		return mix.getRadiation();
	}
}
