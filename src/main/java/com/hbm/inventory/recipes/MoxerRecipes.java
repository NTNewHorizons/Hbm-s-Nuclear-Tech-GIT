package com.hbm.inventory.recipes;

import com.hbm.inventory.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.item.ItemStack;

public class MoxerRecipes extends GenericRecipes<MoxerRecipe> {

	public static final MoxerRecipes INSTANCE = new MoxerRecipes();
	@Override
	public int inputItemLimit() { return 0; }

	@Override
	public int inputFluidLimit() { return 2; }

	@Override
	public int outputItemLimit() { return 1; }

	@Override
	public int outputFluidLimit() { return 0; }

	@Override
	public MoxerRecipe instantiateRecipe(String name) { return new MoxerRecipe(name); }

	@Override
	public String getFileName() { return "hbmMoxer.json"; }

	@Override
	public void registerDefaults() {
		this.register((MoxerRecipe) new MoxerRecipe("moxer.ingot").setAmount(72).setup(600, 15000).setNameWrapper("moxer.ingot")
			.inputFluids(new FluidStack(Fluids.NITRIC_ACID, 75), new FluidStack(Fluids.AMMONIA, 75), new FluidStack(Fluids.KRYPTON, 150))
			.outputItems(new ItemStack(ModItems.custom_fuel_ingot))
			.setIcon(ModItems.ingot_uranium_fuel));
		this.register((MoxerRecipe) new MoxerRecipe("moxer.billet").setAmount(48).setup(400, 15000).setNameWrapper("moxer.billet")
			.inputFluids(new FluidStack(Fluids.NITRIC_ACID, 50), new FluidStack(Fluids.AMMONIA, 50), new FluidStack(Fluids.KRYPTON, 100))
			.outputItems(new ItemStack(ModItems.custom_fuel_billet))
			.setIcon(ModItems.billet_uranium_fuel));
		this.register((MoxerRecipe) new MoxerRecipe("moxer.rbmk").setAmount(384).setup(3200, 15000).setNameWrapper("moxer.rbmk")
			.inputFluids(new FluidStack(Fluids.NITRIC_ACID, 400), new FluidStack(Fluids.AMMONIA, 400), new FluidStack(Fluids.KRYPTON, 800))
			.outputItems(new ItemStack(ModItems.custom_fuel_rbmk_rod))
			.setIcon(ModItems.rbmk_fuel_meu)
			.setPools(GenericRecipes.POOL_PREFIX_ALT + "moxer"));
	}
}
