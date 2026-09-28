package com.hbm.handler.nei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.AtmosphereRecipes;

public class AtmosphereRecipeHandler extends NEIUniversalHandler {

	public AtmosphereRecipeHandler() {
		super("nei.atmosphere_recipe_handler.name", ModBlocks.machine_atmo_emitter, AtmosphereRecipes.getRecipes());
	}

	@Override
	public String getKey() {
		return "ntmAtmoChem";
	}
	
}
