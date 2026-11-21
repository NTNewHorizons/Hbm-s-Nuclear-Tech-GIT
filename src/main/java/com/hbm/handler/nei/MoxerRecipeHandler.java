package com.hbm.handler.nei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.MoxerRecipes;

public class MoxerRecipeHandler extends NEIGenericRecipeHandler {

	public MoxerRecipeHandler() {
		super(ModBlocks.machine_moxer.getLocalizedName(), MoxerRecipes.INSTANCE, ModBlocks.machine_moxer);
	}

	@Override public String getRecipeID() { return "ntmMoxer"; }
}
