package com.hbm.inventory.recipes;

import com.hbm.inventory.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ICustomFuelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MoxerRecipes extends GenericRecipes<MoxerRecipe> {

	public static final MoxerRecipes INSTANCE = new MoxerRecipes();
	@Override
	public int inputItemLimit() { return 0; }

	@Override
	public int inputFluidLimit() { return 3; }

	@Override
	public int outputItemLimit() { return 1; }

	@Override
	public int outputFluidLimit() { return 0; }

	@Override
	public MoxerRecipe instantiateRecipe(String name) { return new MoxerRecipe(name); }

	@Override
	public String getFileName() { return "hbmMoxer.json"; }

	// the real mix is set by the Moxer, this one only shows up in NEI
	private static ItemStack display(Item item) {
		return ICustomFuelItem.makeSample(item);
	}

	@Override
	public void registerDefaults() {
		this.register((MoxerRecipe) new MoxerRecipe("moxer.ingot").setAmount(72).setup(600, 15000).setNameWrapper("moxer.ingot")
			.inputFluids(new FluidStack(Fluids.NITRIC_ACID, 75), new FluidStack(Fluids.AMMONIA, 75), new FluidStack(Fluids.KRYPTON, 150))
			.outputItems(display(ModItems.custom_fuel_ingot))
			.setIcon(ModItems.ingot_uranium_fuel));
		this.register((MoxerRecipe) new MoxerRecipe("moxer.billet").setAmount(48).setup(400, 15000).setNameWrapper("moxer.billet")
			.inputFluids(new FluidStack(Fluids.NITRIC_ACID, 50), new FluidStack(Fluids.AMMONIA, 50), new FluidStack(Fluids.KRYPTON, 100))
			.outputItems(display(ModItems.custom_fuel_billet))
			.setIcon(ModItems.billet_uranium_fuel));
	}
}
