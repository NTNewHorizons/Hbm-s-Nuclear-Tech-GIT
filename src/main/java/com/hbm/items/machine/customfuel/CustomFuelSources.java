package com.hbm.items.machine.customfuel;

import static com.hbm.items.machine.customfuel.EnumCustomFuel.*;

import java.util.HashMap;
import java.util.Map;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

// What the Moxer gets out of non-custom items, mixed materials are split into their isotopes using the vanilla crafting ratios
public class CustomFuelSources {

	// isotope / weight pairs, a null isotope is material that isn't fuel (like the beryllium in schrabidium fuel)
	private static final Map<NTMMaterial, Object[]> mixedMaterials = new HashMap();
	private static final Map<Item, Object[]> mixedItems = new HashMap();
	private static final Map<Item, Integer> itemQuanta = new HashMap();

	private static void initIfNeeded() {
		if(!mixedMaterials.isEmpty()) return;

		mixedMaterials.put(Mats.MAT_URANIUM, new Object[] { U235, 1, U238, 11 });
		mixedMaterials.put(Mats.MAT_PLUTONIUM, new Object[] { PU238, 1, PU239, 6, PU240, 3 });
		mixedMaterials.put(Mats.MAT_RGP, new Object[] { PU239, 2, PU240, 1 });
		mixedMaterials.put(Mats.MAT_RGA, new Object[] { AM241, 1, AM242, 2 });

		mixed(new Object[] { U235, 1, U238, 5 }, ModItems.nugget_uranium_fuel, ModItems.billet_uranium_fuel, ModItems.ingot_uranium_fuel);
		mixed(new Object[] { U233, 1, TH232, 5 }, ModItems.nugget_thorium_fuel, ModItems.billet_thorium_fuel, ModItems.ingot_thorium_fuel);
		mixed(new Object[] { U238, 6, PU239, 2, PU240, 1 }, ModItems.nugget_plutonium_fuel, ModItems.billet_plutonium_fuel, ModItems.ingot_plutonium_fuel);
		mixed(new Object[] { U235, 1, U238, 5, PU239, 3 }, ModItems.nugget_mox_fuel, ModItems.billet_mox_fuel, ModItems.ingot_mox_fuel);
		mixed(new Object[] { U238, 2, NP237, 1 }, ModItems.nugget_neptunium_fuel, ModItems.billet_neptunium_fuel, ModItems.ingot_neptunium_fuel);
		mixed(new Object[] { U238, 6, AM241, 1, AM242, 2 }, ModItems.nugget_americium_fuel, ModItems.billet_americium_fuel, ModItems.ingot_americium_fuel);
		mixed(new Object[] { SA326, 1, NP237, 1, null, 1 }, ModItems.nugget_schrabidium_fuel, ModItems.billet_schrabidium_fuel, ModItems.ingot_schrabidium_fuel);
		mixed(new Object[] { CM244, 1, CM245, 2 }, ModItems.nugget_cm_mix, ModItems.billet_cm_mix, ModItems.ingot_cm_mix);
		mixed(new Object[] { U238, 6, CM244, 1, CM245, 2 }, ModItems.nugget_cm_fuel, ModItems.billet_cm_fuel, ModItems.ingot_cm_fuel);
	}

	private static void mixed(Object[] shares, Item nugget, Item billet, Item ingot) {
		mixedItems.put(nugget, shares); itemQuanta.put(nugget, MaterialShapes.NUGGET.q(1));
		mixedItems.put(billet, shares); itemQuanta.put(billet, MaterialShapes.BILLET.q(1));
		mixedItems.put(ingot, shares); itemQuanta.put(ingot, MaterialShapes.INGOT.q(1));
	}

	private static void split(CustomFuelMix mix, Object[] shares, int amount) {
		int total = 0;
		for(int i = 0; i < shares.length / 2; i++) total += (Integer) shares[i * 2 + 1];
		for(int i = 0; i < shares.length / 2; i++) {
			EnumCustomFuel fuel = (EnumCustomFuel) shares[i * 2];
			if(fuel != null) mix.amounts[fuel.ordinal()] += amount * (Integer) shares[i * 2 + 1] / total;
		}
	}

	// null if the item holds no fuel isotopes at all
	public static CustomFuelMix dissolve(ItemStack stack) {
		if(stack == null) return null;
		initIfNeeded();
		CustomFuelMix mix = new CustomFuelMix();

		Object[] itemShares = mixedItems.get(stack.getItem());
		if(itemShares != null) {
			split(mix, itemShares, itemQuanta.get(stack.getItem()));
		} else {
			for(MaterialStack mat : Mats.getSmeltingMaterialsFromItem(stack)) {
				EnumCustomFuel fuel = EnumCustomFuel.byMaterial(mat.material);
				if(fuel != null) mix.amounts[fuel.ordinal()] += mat.amount;
				else if(mixedMaterials.containsKey(mat.material)) split(mix, mixedMaterials.get(mat.material), mat.amount);
			}
		}

		if(mix.total() <= 0) return null;
		mix.fresh = mix.total();
		return mix;
	}
}
