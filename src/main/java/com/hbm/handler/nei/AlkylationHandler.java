package com.hbm.handler.nei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.AlkylationRecipes;

public class AlkylationHandler extends NEIUniversalHandler {

	public AlkylationHandler() {
		super("nei.alkylation_handler.name", ModBlocks.machine_alkylation, AlkylationRecipes.getRecipes());
	}

	@Override
	public String getKey() {
		return "ntmAlkylation";
	}
}
