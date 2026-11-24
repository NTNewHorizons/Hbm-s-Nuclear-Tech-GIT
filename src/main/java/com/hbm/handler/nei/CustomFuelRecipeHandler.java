package com.hbm.handler.nei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm.crafting.handlers.CustomFuelCraftingHandler;
import com.hbm.crafting.handlers.CustomFuelCraftingHandler.Def;
import com.hbm.items.machine.ICustomFuelItem;
import com.hbm.items.machine.customfuel.CustomFuelMix;
import com.hbm.util.i18n.I18nUtil;

import codechicken.nei.recipe.ShapedRecipeHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import static com.hbm.crafting.handlers.CustomFuelCraftingHandler.CUSTOM;
import static com.hbm.crafting.handlers.CustomFuelCraftingHandler.getDefs;

// Custom fuel crafting is NBT based, so the recipes are built for the exact mix that was looked up
public class CustomFuelRecipeHandler extends ShapedRecipeHandler {

	private static final String ID = "hbmCustomFuelCrafting";

	@Override public String getRecipeName() { return I18nUtil.resolveKey("nei.custom_fuel_crafting.name"); }
	@Override public String getHandlerId() { return ID; }

	@Override
	public void loadTransferRects() {
		transferRects.add(new RecipeTransferRect(new Rectangle(84, 23, 24, 18), ID));
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if(!outputId.equals(ID)) { super.loadCraftingRecipes(outputId, results); return; }
		for(Def def : getDefs()) addForInput(def, preset(def.custom));
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		if(result == null || !(result.getItem() instanceof ICustomFuelItem) || ICustomFuelItem.isSample(result)) return;
		CustomFuelMix mix = CustomFuelMix.fromStack(result);

		for(Def def : getDefs()) {
			if(def.output != result.getItem()) continue;
			if(mix == null) { addForInput(def, preset(def.custom)); continue; }

			// whatever the inputs held gets split across the outputs, so scale it back up
			int count = def.customCount();
			CustomFuelMix input = new CustomFuelMix();
			for(int i = 0; i < mix.amounts.length; i++) input.amounts[i] = mix.amounts[i] * def.outCount / count;
			input.fresh = mix.fresh * def.outCount / count;
			addForInput(def, input.writeTo(new ItemStack(def.custom)));
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		if(ingredient == null || ICustomFuelItem.isSample(ingredient)) return;

		for(Def def : getDefs()) {
			if(ingredient.getItem() == def.custom) {
				ItemStack input = CustomFuelMix.hasMix(ingredient) ? ingredient.copy() : preset(def.custom);
				input.stackSize = 1;
				addForInput(def, input);
			} else {
				for(Object value : def.values) {
					if(matches(ingredient, value)) { addForInput(def, preset(def.custom)); break; }
				}
			}
		}
	}

	private void addForInput(Def def, ItemStack custom) {
		List<ItemStack> customs = Collections.nCopies(def.customCount(), custom);
		ItemStack out = CustomFuelCraftingHandler.craft(def, customs);
		if(out == null) return;

		int width, height;
		Object[] grid;

		if(def.shape != null) {
			height = def.shape.length;
			width = def.shape[0].length();
			grid = new Object[width * height];
			for(int y = 0; y < height; y++) for(int x = 0; x < width; x++) {
				char c = def.shape[y].charAt(x);
				grid[y * width + x] = c == ' ' ? null : c == CUSTOM ? custom : display(def.valueOf(c));
			}
		} else {
			// shapeless: the fixed item goes in the middle of the customs, like the vanilla rod recipes
			List<Object> fixed = new ArrayList();
			for(Object value : def.values) if(!(value instanceof Character && (Character) value == CUSTOM)) fixed.add(display(value));
			int count = def.customCount();
			List<Object> items = new ArrayList();
			for(int i = 0; i < count / 2; i++) items.add(custom);
			items.addAll(fixed);
			for(int i = count / 2; i < count; i++) items.add(custom);

			width = Math.min(items.size(), 3);
			height = (items.size() + 2) / 3;
			grid = items.toArray(new Object[width * height]);
		}

		CachedShapedRecipe recipe = new CachedShapedRecipe(width, height, grid, out);
		recipe.computeVisuals();
		arecipes.add(recipe);
	}

	private static Object display(Object value) {
		if(value instanceof Item) return new ItemStack((Item) value);
		if(value instanceof String) return OreDictionary.getOres((String) value);
		return value;
	}

	private static boolean matches(ItemStack stack, Object value) {
		if(value instanceof Item) return stack.getItem() == value;
		if(value instanceof String) {
			int id = OreDictionary.getOreID((String) value);
			for(int i : OreDictionary.getOreIDs(stack)) if(i == id) return true;
		}
		return false;
	}

	private static ItemStack preset(Item item) {
		return ICustomFuelItem.makePreset(item, ICustomFuelItem.PRESETS[0]);
	}
}
