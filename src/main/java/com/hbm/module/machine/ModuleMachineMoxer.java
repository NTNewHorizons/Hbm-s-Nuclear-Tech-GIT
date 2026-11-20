package com.hbm.module.machine;

import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.recipes.MoxerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;

import api.hbm.energymk2.IEnergyHandlerMK2;
import net.minecraft.item.ItemStack;

public class ModuleMachineMoxer extends ModuleMachineBase {

	public ModuleMachineMoxer(int index, IEnergyHandlerMK2 battery, ItemStack[] slots) {
		super(index, battery, slots);
		this.inputTanks = new FluidTank[3];
		this.outputTanks = new FluidTank[0];
		this.outputSlots = new int[1];
	}

	@Override
	public GenericRecipes getRecipeSet() {
		return MoxerRecipes.INSTANCE;
	}

	@Override
	public void setupTanks(GenericRecipe recipe) {
		super.setupTanks(recipe);
		if(recipe == null) return;
		for(int i = 0; i < inputTanks.length; i++) if(recipe.inputFluid != null && recipe.inputFluid.length > i) inputTanks[i].conform(recipe.inputFluid[i]); else inputTanks[i].resetTank();
	}

	public ModuleMachineMoxer itemOutput(int start) { for(int i = 0; i < outputSlots.length; i++) outputSlots[i] = start + i; return this; }
	public ModuleMachineMoxer fluidInput(FluidTank a, FluidTank b, FluidTank c) { inputTanks[0] = a; inputTanks[1] = b; inputTanks[2] = c; return this; }
}
