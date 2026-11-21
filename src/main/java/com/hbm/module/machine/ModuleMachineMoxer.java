package com.hbm.module.machine;

import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.recipes.MoxerRecipe;
import com.hbm.inventory.recipes.MoxerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ICustomFuelItem;
import com.hbm.items.machine.ItemCustomRBMKRod;
import com.hbm.items.machine.customfuel.CustomFuelMix;

import api.hbm.energymk2.IEnergyHandlerMK2;
import net.minecraft.item.ItemStack;

// Instead of input items the Moxer uses its isotope buffer, perCycle is what one cycle takes out of it
public class ModuleMachineMoxer extends ModuleMachineBase {

	protected CustomFuelMix buffer;
	protected CustomFuelMix perCycle;

	public ModuleMachineMoxer(int index, IEnergyHandlerMK2 battery, ItemStack[] slots) {
		super(index, battery, slots);
		this.inputSlots = new int[0];
		this.inputTanks = new FluidTank[3];
		this.outputTanks = new FluidTank[0];
		this.outputSlots = new int[1];
	}

	@Override
	public GenericRecipes getRecipeSet() {
		return MoxerRecipes.INSTANCE;
	}

	public ItemStack buildProduct(GenericRecipe recipe) {
		if(recipe == null || recipe.outputItem == null || recipe.outputItem.length == 0) return null;
		ItemStack out = recipe.outputItem[0].collapse();
		if(out == null) return null;
		out = out.copy();
		out.stackSize = 1;

		if(out.getItem() instanceof ICustomFuelItem) {
			CustomFuelMix mix = perCycle.copy();
			mix.fresh = ((MoxerRecipe) recipe).amount;
			mix.writeTo(out);
			if(out.getItem() instanceof ItemCustomRBMKRod) ItemCustomRBMKRod.prepare(out);
		}
		return out;
	}

	@Override
	protected boolean hasInput(GenericRecipe recipe) {
		if(!super.hasInput(recipe)) return false;
		for(int i = 0; i < perCycle.amounts.length; i++) if(buffer.amounts[i] < perCycle.amounts[i]) return false;
		return perCycle.total() >= ((MoxerRecipe) recipe).amount;
	}

	@Override
	protected boolean canFitOutput(GenericRecipe recipe) {
		ItemStack product = buildProduct(recipe);
		if(product == null) return false;
		ItemStack current = slots[outputSlots[0]];
		if(current == null) return true;
		return current.getItem() == product.getItem()
				&& current.getItemDamage() == product.getItemDamage()
				&& ItemStack.areItemStackTagsEqual(current, product)
				&& current.stackSize < current.getMaxStackSize();
	}

	@Override
	protected void consumeInput(GenericRecipe recipe) {
		super.consumeInput(recipe);
		for(int i = 0; i < perCycle.amounts.length; i++) buffer.amounts[i] -= perCycle.amounts[i];
	}

	@Override
	protected void produceItem(GenericRecipe recipe) {
		ItemStack product = buildProduct(recipe);
		if(slots[outputSlots[0]] == null) slots[outputSlots[0]] = product;
		else slots[outputSlots[0]].stackSize++;
		this.markDirty = true;
	}

	public ModuleMachineMoxer buffer(CustomFuelMix buffer, CustomFuelMix perCycle) { this.buffer = buffer; this.perCycle = perCycle; return this; }
	public ModuleMachineMoxer itemOutput(int start) { for(int i = 0; i < outputSlots.length; i++) outputSlots[i] = start + i; return this; }
	public ModuleMachineMoxer fluidInput(FluidTank a, FluidTank b, FluidTank c) { inputTanks[0] = a; inputTanks[1] = b; inputTanks[2] = c; return this; }
}
