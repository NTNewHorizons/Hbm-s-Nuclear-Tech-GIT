package com.hbm.inventory.recipes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.recipes.loader.SerializableRecipe;

import com.hbm.inventory.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.util.Tuple.Pair;

import net.minecraft.item.ItemStack;

public class RadiolysisRecipes extends SerializableRecipe {
	//All cracking recipes, + 2H2O => H2O2 + H2, + heavy oil => light oil + waste product, and others i haven't even thought of yet
	
	private static Map<FluidType, Pair<FluidStack, FluidStack>> radiolysis = new HashMap(); //fluidstacks :reimumunch:

	@Override public String getFileName() { return "hbmRadiolysis.json"; }
	@Override public Object getRecipeObject() { return radiolysis; }
	@Override public void deleteRecipes() { radiolysis.clear(); }
	@Override public void registerDefaults() { registerRadiolysis(); }
	@Override public String getComment() {
		return "Inputs are always 100mB, set output quantities accordingly. Cracking recipes are only added automatically while this file is not enabled.";
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = recipe.getAsJsonObject();
		radiolysis.put(Fluids.fromName(obj.get("input").getAsString()), new Pair(
				readFluidStack(obj.getAsJsonArray("output1")), readFluidStack(obj.getAsJsonArray("output2"))));
	}

	@Override
	public void writeRecipe(Object recipe, JsonWriter writer) throws IOException {
		Entry<FluidType, Pair<FluidStack, FluidStack>> rec = (Entry<FluidType, Pair<FluidStack, FluidStack>>) recipe;
		writer.name("input").value(rec.getKey().getName());
		writer.name("output1"); writeFluidStack(rec.getValue().getKey(), writer);
		writer.name("output2"); writeFluidStack(rec.getValue().getValue(), writer);
	}
	
	/* I am proud of this */
	public static Map<Object, Object[]> getRecipesForNEI() {
		Map<Object, Object[]> recipes = new HashMap<Object, Object[]>();
		Iterator itr = radiolysis.entrySet().iterator();
		
		while(itr.hasNext()) {
			Map.Entry entry = (Entry) itr.next();
			Pair<FluidStack, FluidStack> pair = (Pair<FluidStack, FluidStack>) entry.getValue();
			ItemStack[] outputs = new ItemStack[2];
			if(pair.getKey().type == Fluids.NONE) {
				outputs[0] = new ItemStack(ModItems.nothing);
			} else {
				outputs[0] = ItemFluidIcon.make(pair.getKey().type, pair.getKey().fill);
			}
			if(pair.getValue().type == Fluids.NONE) {
				outputs[1] = new ItemStack(ModItems.nothing);
			} else {
				outputs[1] = ItemFluidIcon.make(pair.getValue().type, pair.getValue().fill);
			}
			
			recipes.put(ItemFluidIcon.make((FluidType) entry.getKey(), 100), outputs);
		}
		
		return recipes;
	}
	
	public static void registerRadiolysis() {
		radiolysis.put(Fluids.WATER, new Pair(new FluidStack(80, Fluids.PEROXIDE), new FluidStack(20, Fluids.HYDROGEN)));
		
		//automatically add cracking recipes to the radiolysis recipe list
		//we want the numbers and types to stay consistent anyway and this will save us a lot of headache later on
		Map<FluidType, Pair<FluidStack, FluidStack>> cracking = CrackingRecipes.getCrackingRecipes();
		
		for(Entry<FluidType, Pair<FluidStack, FluidStack>> recipe : cracking.entrySet()) {
			radiolysis.put(recipe.getKey(), recipe.getValue());
		}
	}
	
	public static Pair<FluidStack, FluidStack> getRadiolysis(FluidType input) {
		return radiolysis.get(input);
	}
}
