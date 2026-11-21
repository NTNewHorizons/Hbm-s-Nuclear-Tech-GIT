package com.hbm.module.machine;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.CustomFuelPUREXRecipe;
import java.util.List;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;

import api.hbm.energymk2.IEnergyHandlerMK2;
import net.minecraft.item.ItemStack;

public class ModuleMachinePUREX extends ModuleMachineBase {

	public ModuleMachinePUREX(int index, IEnergyHandlerMK2 battery, ItemStack[] slots) {
		super(index, battery, slots);
		this.inputSlots = new int[3];
		this.outputSlots = new int[6];
		this.inputTanks = new FluidTank[3];
		this.outputTanks = new FluidTank[1];
	}

	// outputs of a custom fuel recipe, read from the input before it gets used up
	protected List<ItemStack> pendingCustom;

	@Override
	protected boolean hasInput(GenericRecipe recipe) {
		if(recipe instanceof CustomFuelPUREXRecipe && CustomFuelPUREXRecipe.getOutputs(slots[inputSlots[0]]) == null) return false;
		return super.hasInput(recipe);
	}

	@Override
	protected boolean canFitOutput(GenericRecipe recipe) {
		if(!(recipe instanceof CustomFuelPUREXRecipe)) return super.canFitOutput(recipe);
		List<ItemStack> outputs = CustomFuelPUREXRecipe.getOutputs(slots[inputSlots[0]]);
		if(outputs == null || outputs.size() > outputSlots.length) return false;
		for(int i = 0; i < outputs.size(); i++) {
			ItemStack slot = slots[outputSlots[i]];
			ItemStack out = outputs.get(i);
			if(slot == null) continue;
			if(slot.getItem() != out.getItem() || slot.getItemDamage() != out.getItemDamage()) return false;
			if(slot.stackSize + out.stackSize > slot.getMaxStackSize()) return false;
		}
		return true;
	}

	@Override
	protected void consumeInput(GenericRecipe recipe) {
		if(recipe instanceof CustomFuelPUREXRecipe) pendingCustom = CustomFuelPUREXRecipe.getOutputs(slots[inputSlots[0]]);
		super.consumeInput(recipe);
	}

	@Override
	protected void produceItem(GenericRecipe recipe) {
		if(!(recipe instanceof CustomFuelPUREXRecipe)) {
			super.produceItem(recipe);
			return;
		}
		if(pendingCustom != null) {
			for(int i = 0; i < Math.min(pendingCustom.size(), outputSlots.length); i++) {
				ItemStack out = pendingCustom.get(i);
				if(slots[outputSlots[i]] == null) slots[outputSlots[i]] = out.copy();
				else slots[outputSlots[i]].stackSize += out.stackSize;
			}
			pendingCustom = null;
		}
		this.markDirty = true;
	}

	@Override
	public GenericRecipes getRecipeSet() {
		return PUREXRecipes.INSTANCE;
	}

	public ModuleMachinePUREX itemInput(int start) { for(int i = 0; i < inputSlots.length; i++) inputSlots[i] = start + i; return this; }
	public ModuleMachinePUREX itemOutput(int start) { for(int i = 0; i < outputSlots.length; i++) outputSlots[i] = start + i; return this; }
	public ModuleMachinePUREX fluidInput(FluidTank a, FluidTank b, FluidTank c) { inputTanks[0] = a; inputTanks[1] = b; inputTanks[2] = c; return this; }
	public ModuleMachinePUREX fluidOutput(FluidTank a) { outputTanks[0] = a; return this; }
}
