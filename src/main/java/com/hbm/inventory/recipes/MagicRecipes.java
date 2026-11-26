package com.hbm.inventory.recipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.recipes.loader.SerializableRecipe;

import static com.hbm.inventory.OreDictManager.*;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.NBTStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModItems;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MagicRecipes extends SerializableRecipe {

	private static List<MagicRecipe> recipes = new ArrayList();

	@Override public String getFileName() { return "hbmMagic.json"; }
	@Override public Object getRecipeObject() { return recipes; }
	@Override public void deleteRecipes() { recipes.clear(); }
	@Override public void registerDefaults() { register(); }
	@Override public String getComment() { return "Input stack sizes are ignored. Input order matters, empty slots are skipped."; }

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = recipe.getAsJsonObject();
		AStack[] inputs = readAStackArray(obj.getAsJsonArray("inputs"));
		if(inputs.length > 4) throw new IllegalArgumentException("Magic recipes accept at most four inputs");
		for(AStack input : inputs) {
			if(input instanceof NBTStack) throw new IllegalArgumentException("Magic recipe inputs must be item or dict stacks");
			input.stacksize = 1;
		}
		recipes.add(new MagicRecipe(readItemStack(obj.getAsJsonArray("output")), inputs));
	}

	@Override
	public void writeRecipe(Object recipe, JsonWriter writer) throws IOException {
		MagicRecipe rec = (MagicRecipe) recipe;
		writer.name("inputs").beginArray();
		for(AStack input : rec.in) writeAStack(input, writer);
		writer.endArray();
		writer.name("output"); writeItemStack(rec.out, writer);
	}

	public static ItemStack getRecipe(InventoryCrafting matrix) {

		List<ComparableStack> comps = new ArrayList();

		for(int i = 0; i < 4; i++) {
			if(matrix.getStackInSlot(i) != null)
				comps.add(new ComparableStack(matrix.getStackInSlot(i)).makeSingular());
		}

		//Collections.sort(comps);

		for(MagicRecipe recipe : recipes) {
			if(recipe.matches(comps))
				return recipe.getResult();
		}

		return null;
	}

	public static void register() {
		recipes.add(new MagicRecipe(new ItemStack(ModItems.ingot_u238m2), new ComparableStack(ModItems.ingot_u238m2, 1, 1), new ComparableStack(ModItems.ingot_u238m2, 1, 2), new ComparableStack(ModItems.ingot_u238m2, 1, 3)));
		recipes.add(new MagicRecipe(new ItemStack(ModItems.rod_of_discord), new ComparableStack(Items.ender_pearl), new ComparableStack(Items.blaze_rod), new ComparableStack(ModItems.nugget_euphemium)));
		recipes.add(new MagicRecipe(new ItemStack(ModItems.balefire_and_steel), new OreDictStack(STEEL.ingot()), new ComparableStack(ModItems.egg_balefire_shard)));
		recipes.add(new MagicRecipe(new ItemStack(ModItems.mysteryshovel), new ComparableStack(Items.iron_shovel), new ComparableStack(Items.bone), new ComparableStack(ModItems.ingot_starmetal), new ComparableStack(ModItems.ducttape)));
		recipes.add(new MagicRecipe(new ItemStack(ModItems.ingot_electronium), new ComparableStack(ModItems.pellet_charged), new ComparableStack(ModItems.pellet_charged), new ComparableStack(ModItems.ingot_dineutronium), new ComparableStack(ModItems.ingot_dineutronium)));

		recipes.add(new MagicRecipe(new ItemStack(ModItems.diamond_gavel),
				new ComparableStack(ModBlocks.gravel_diamond),
				new ComparableStack(ModBlocks.gravel_diamond),
				new ComparableStack(ModBlocks.gravel_diamond),
				new ComparableStack(ModItems.lead_gavel)));

		recipes.add(new MagicRecipe(new ItemStack(ModItems.mese_gavel),
				new ComparableStack(ModItems.shimmer_handle),
				new ComparableStack(ModItems.powder_dineutronium),
				new ComparableStack(ModItems.blades_desh),
				new ComparableStack(ModItems.diamond_gavel)));

		recipes.add(new MagicRecipe(new ItemStack(Item.getItemFromBlock(ModBlocks.bobblehead), 1, 21),
				new ComparableStack(ModItems.ingot_gwenium),
				new ComparableStack(ModItems.ingot_dineutronium),
				new ComparableStack(ModItems.ingot_dineutronium)));
	}

	public static List<MagicRecipe> getRecipes() {
		return recipes;
	}

	public static class MagicRecipe {

		public List<AStack> in;
		public ItemStack out;

		public MagicRecipe(ItemStack out, AStack... in) {
			this.out = out;
			this.in = Arrays.asList(in);
		}

		public boolean matches(List<ComparableStack> comps) {

			if(comps.size() != in.size())
				return false;

			for(int i = 0; i < in.size(); i++) {

				if(!in.get(i).matchesRecipe(comps.get(i).toStack(), false)) return false;
			}

			return true;
		}

		public ItemStack getResult() {
			return out.copy();
		}
	}
}
