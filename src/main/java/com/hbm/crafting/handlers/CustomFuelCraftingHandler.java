package com.hbm.crafting.handlers;

import java.util.ArrayList;
import java.util.List;

import com.hbm.config.MachineConfig;
import com.hbm.inventory.OreDictManager;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemCustomRBMKRod;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.items.machine.customfuel.CustomFuelSpent;
import com.hbm.items.machine.customfuel.CustomFuelSpent.Reactor;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

// result mix = sum of all custom inputs split across the output, so different batches blend
public class CustomFuelCraftingHandler implements IRecipe {

	public static final char CUSTOM = 'C';

	public static class Def {
		// null for shapeless
		public final String[] shape;
		public final char[] keys;
		public final Object[] values;
		public final Item custom;
		public final Item output;
		public final int outCount;

		public int customCount() {
			if(shape == null) {
				int count = 0;
				for(Object value : values) if(value instanceof Character && (Character) value == CUSTOM) count++;
				return count;
			}
			int count = 0;
			for(String row : shape) for(char c : row.toCharArray()) if(c == CUSTOM) count++;
			return count;
		}

		Def(String[] shape, Item custom, Item output, int outCount, Object... keyValues) {
			this.shape = shape;
			this.custom = custom;
			this.output = output;
			this.outCount = outCount;
			this.keys = new char[keyValues.length / 2];
			this.values = new Object[keyValues.length / 2];
			for(int i = 0; i < keys.length; i++) {
				keys[i] = (Character) keyValues[i * 2];
				values[i] = keyValues[i * 2 + 1];
			}
		}

		public Object valueOf(char c) {
			for(int i = 0; i < keys.length; i++) if(keys[i] == c) return values[i];
			return null;
		}
	}

	private static final List<Def> defs = new ArrayList();

	public static List<Def> getDefs() {
		if(defs.isEmpty()) {
			defs.add(new Def(null, ModItems.custom_fuel_billet, ModItems.custom_fuel_ingot, 2, 'a', CUSTOM, 'b', CUSTOM, 'c', CUSTOM));
			defs.add(new Def(new String[] { "CC" }, ModItems.custom_fuel_ingot, ModItems.custom_fuel_billet, 3));
			defs.add(new Def(null, ModItems.custom_fuel_billet, ModItems.custom_fuel_rbmk_rod, 1, 'e', ModItems.rbmk_fuel_empty,
					'a', CUSTOM, 'b', CUSTOM, 'c', CUSTOM, 'd', CUSTOM, 'f', CUSTOM, 'g', CUSTOM, 'h', CUSTOM, 'i', CUSTOM));
			defs.add(new Def(null, ModItems.custom_fuel_billet, ModItems.custom_fuel_zirnox, 1, 'e', ModItems.rod_zirnox_empty, 'a', CUSTOM, 'b', CUSTOM));
			defs.add(new Def(new String[] { "C", "P", "C" }, ModItems.custom_fuel_billet, ModItems.custom_fuel_pwr, 1, 'P', ModItems.plate_polymer));
			defs.add(new Def(new String[] { " C ", "CGC", " C " }, ModItems.custom_fuel_ingot, ModItems.custom_fuel_watz, 1, 'G', OreDictManager.GRAPHITE.ingot()));
		}
		return defs;
	}

	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		return getCraftingResult(inv) != null;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {

		ItemStack single = getSingleStack(inv);
		if(single != null) {
			if(single.getItem() instanceof ItemCustomRBMKRod) return breakRod(single);
			if(MachineConfig.customFuelVanillaCraft() && CustomFuelSpent.isSpent(single) && single.getItem() != ModItems.custom_fuel_pwr_hot) return toVanilla(single);
		}

		for(Def def : getDefs()) {
			List<ItemStack> customs = def.shape != null ? matchShaped(def, inv) : matchShapeless(def, inv);
			if(customs == null) continue;

			return craft(def, customs);
		}

		return null;
	}

	public static ItemStack craft(Def def, List<ItemStack> customs) {
		CustomFuelMix sum = new CustomFuelMix();
		for(ItemStack stack : customs) {
			CustomFuelMix mix = CustomFuelMix.fromStack(stack);
			if(mix == null) return null;
			sum.add(mix);
		}

		ItemStack out = sum.divide(def.outCount).writeTo(new ItemStack(def.output, def.outCount));
		if(def.output == ModItems.custom_fuel_rbmk_rod) ItemCustomRBMKRod.prepare(out);
		return out;
	}

	private ItemStack breakRod(ItemStack rod) {
		CustomFuelMix mix = CustomFuelMix.fromStack(rod);
		if(mix == null) return null;
		if(ItemRBMKRod.getHullHeat(rod) >= 50 || ItemRBMKRod.getCoreHeat(rod) >= 50) return null;
		double enrichment = ItemRBMKRod.getEnrichment(rod);
		if(enrichment > 0.99D) return null;

		// same depletion stages as vanilla pellets
		int stage = 4 - MathHelper.clamp_int((int) Math.ceil(enrichment * 5 - 1), 0, 4);
		int xenon = ItemRBMKRod.getPoisonLevel(rod) >= 0.5D ? 5 : 0;
		CustomFuelMix burned = mix.burnUp(1D - enrichment);

		if(MachineConfig.customFuelDirectVanilla()) {
			ItemStack vanilla = CustomFuelSpent.closestVanilla(Reactor.RBMK, burned, stage);
			if(vanilla == null) return CustomFuelSpent.waste(burned.fresh);
			vanilla.stackSize = 8;
			vanilla.setItemDamage(stage + xenon);
			return vanilla;
		}

		ItemStack pellets = burned.divide(8).writeTo(new ItemStack(ModItems.custom_fuel_rbmk_pellet, 8, stage + xenon));
		return pellets;
	}

	private ItemStack toVanilla(ItemStack spent) {
		CustomFuelMix mix = CustomFuelMix.fromStack(spent);
		Reactor reactor = CustomFuelSpent.reactorOf(spent.getItem());
		if(mix == null || reactor == null) return null;
		int meta = ItemRBMKPellet.rectify(spent.getItemDamage());
		ItemStack vanilla = CustomFuelSpent.closestVanilla(reactor, mix, meta % 5);
		if(vanilla == null) return null;
		if(reactor == Reactor.RBMK) vanilla.setItemDamage(meta);
		return vanilla;
	}

	private static boolean isFreshCustom(ItemStack stack, Item custom) {
		return stack != null && stack.getItem() == custom && CustomFuelMix.hasMix(stack);
	}

	private static boolean matchesFixed(ItemStack stack, Object value) {
		if(stack == null) return false;
		if(value instanceof Item) return stack.getItem() == value;
		if(value instanceof String) {
			int id = OreDictionary.getOreID((String) value);
			for(int i : OreDictionary.getOreIDs(stack)) if(i == id) return true;
		}
		return false;
	}

	private List<ItemStack> matchShapeless(Def def, InventoryCrafting inv) {
		List<ItemStack> stacks = new ArrayList();
		for(int i = 0; i < inv.getSizeInventory(); i++) if(inv.getStackInSlot(i) != null) stacks.add(inv.getStackInSlot(i));
		if(stacks.size() != def.values.length) return null;

		List<ItemStack> customs = new ArrayList();
		List<Object> fixed = new ArrayList();
		int customCount = 0;
		for(Object value : def.values) {
			if(value instanceof Character && (Character) value == CUSTOM) customCount++;
			else fixed.add(value);
		}

		for(ItemStack stack : stacks) {
			if(isFreshCustom(stack, def.custom)) {
				customs.add(stack);
				continue;
			}
			boolean found = false;
			for(int i = 0; i < fixed.size(); i++) {
				if(matchesFixed(stack, fixed.get(i))) {
					fixed.remove(i);
					found = true;
					break;
				}
			}
			if(!found) return null;
		}

		return fixed.isEmpty() && customs.size() == customCount ? customs : null;
	}

	private List<ItemStack> matchShaped(Def def, InventoryCrafting inv) {
		int width = 3, height = 3;
		int minX = width, minY = height, maxX = -1, maxY = -1;
		for(int x = 0; x < width; x++) for(int y = 0; y < height; y++) {
			if(inv.getStackInRowAndColumn(x, y) == null) continue;
			minX = Math.min(minX, x); maxX = Math.max(maxX, x);
			minY = Math.min(minY, y); maxY = Math.max(maxY, y);
		}
		if(maxX < 0) return null;
		if(maxY - minY + 1 != def.shape.length || maxX - minX + 1 != def.shape[0].length()) return null;

		List<ItemStack> customs = new ArrayList();
		for(int y = 0; y < def.shape.length; y++) for(int x = 0; x < def.shape[y].length(); x++) {
			char c = def.shape[y].charAt(x);
			ItemStack stack = inv.getStackInRowAndColumn(minX + x, minY + y);
			if(c == ' ') {
				if(stack != null) return null;
			} else if(c == CUSTOM) {
				if(!isFreshCustom(stack, def.custom)) return null;
				customs.add(stack);
			} else if(!matchesFixed(stack, def.valueOf(c))) {
				return null;
			}
		}
		return customs;
	}

	private static ItemStack getSingleStack(InventoryCrafting inv) {
		ItemStack found = null;
		for(int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(stack == null) continue;
			if(found != null) return null;
			found = stack;
		}
		return found;
	}

	@Override
	public int getRecipeSize() {
		return 9;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return null;
	}
}
