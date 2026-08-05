package com.hbm.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.hbm.crafting.handlers.ItemToolRecipe;

import api.hbm.block.IToolable.ToolType;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;

/**
 * Tool-based crafting recipes.
 *
 * These recipes require the player to place specific tools in any empty crafting grid slot.
 * Tools take durability damage based on recipe complexity (unique ingredients + output count).
 *
 * Shaped usage:
 *   addShaped(new ItemStack(YourBlock), new Object[]{
 *       "III", "I I", "BBB",
 *       'I', Items.brick,
 *       'B', KEY_STONE
 *   }, ToolType.HAMMER);
 *
 * Shaped with minimum tool tier:
 *   addShaped(result, recipe, 2 /*minTier*\/, ToolType.HAMMER);
 *
 * Mirrored shaped:
 *   addShapedMirrored(result, recipe, ToolType.HAMMER, ToolType.SCREWDRIVER);
 *
 * Shapeless:
 *   addShapeless(result, new Object[]{ ModItems.some_ingredient, "ingotIron" }, ToolType.CUTTER);
 *
 * Ingredient types accepted: ItemStack, Item, Block, String (ore dict).
 * Tool types: SCREWDRIVER, HAMMER, SAW, CUTTER, WRENCH, TORCH (welding).
 */
public class ItemToolRecipeHelper {

	public static void addShaped(ItemStack result, Object[] recipe, ToolType... tools) {
		addShaped(result, recipe, 0, tools);
	}

	public static void addShaped(ItemStack result, Object[] recipe, int minTier, ToolType... tools) {
		addShaped0(result, recipe, false, minTier, tools);
	}

	public static void addShapedMirrored(ItemStack result, Object[] recipe, ToolType... tools) {
		addShapedMirrored(result, recipe, 0, tools);
	}

	public static void addShapedMirrored(ItemStack result, Object[] recipe, int minTier, ToolType... tools) {
		addShaped0(result, recipe, true, minTier, tools);
	}

	private static void addShaped0(ItemStack result, Object[] recipe, boolean mirrored, int minTier, ToolType... tools) {
		List<String> rows = new ArrayList<>();
		int idx = 0;
		while(idx < recipe.length && recipe[idx] instanceof String) {
			rows.add((String) recipe[idx]);
			idx++;
		}

		int height = rows.size();
		int width = 0;
		for(String row : rows) {
			if(row.length() > width) width = row.length();
		}

		HashMap<Character, Object> charMap = new HashMap<>();
		while(idx < recipe.length) {
			char key = (Character) recipe[idx++];
			Object value = recipe[idx++];
			charMap.put(key, value);
		}

		Object[] inputs = new Object[9];
		Arrays.fill(inputs, null);

		for(int r = 0; r < height; r++) {
			String row = rows.get(r);
			for(int c = 0; c < row.length(); c++) {
				char ch = row.charAt(c);
				if(ch != ' ') {
					Object ing = charMap.get(ch);
					inputs[r * 3 + c] = ing;
				}
			}
		}

		ItemToolRecipe toolRecipe = new ItemToolRecipe(result, width, height, inputs, minTier, tools);
		if(mirrored) toolRecipe.setMirrored(true);
		GameRegistry.addRecipe(toolRecipe);
	}

	public static void addShapeless(ItemStack result, Object[] inputs, ToolType... tools) {
		addShapeless(result, inputs, 0, tools);
	}

	public static void addShapeless(ItemStack result, Object[] inputs, int minTier, ToolType... tools) {
		List<Object> inputList = new ArrayList<>();
		for(Object o : inputs) {
			if(o != null) inputList.add(o);
		}
		GameRegistry.addRecipe(new ItemToolRecipe(result, inputList, minTier, tools));
	}
}
