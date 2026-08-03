package com.hbm.handler.nei;

import java.util.ArrayList;
import java.util.List;

import com.hbm.crafting.handlers.GergToolRecipe;
import com.hbm.handler.imc.ICompatNHNEI;
import com.hbm.items.tool.GergToolType;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

public class GergToolNEIHandler extends TemplateRecipeHandler implements ICompatNHNEI {

	@Override
	public String getRecipeName() {
		return "Gergified Crafting";
	}

	@Override
	public String getGuiTexture() {
		return "textures/gui/container/crafting_table.png";
	}

	@Override
	public String getOverlayIdentifier() {
		return "crafting";
	}

	@Override
	public Class<? extends GuiContainer> getGuiClass() {
		return GuiCrafting.class;
	}

	@Override
	public ItemStack[] getMachinesForRecipe() {
		return new ItemStack[]{new ItemStack(Blocks.crafting_table)};
	}

	@Override
	public String getRecipeID() {
		return "gergCrafting";
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if(outputId.equals("item") && getClass() == GergToolNEIHandler.class) {
			for(Object o : CraftingManager.getInstance().getRecipeList()) {
				if(o instanceof GergToolRecipe) {
					GergToolRecipe recipe = (GergToolRecipe) o;
					for(Object r : results) {
						if(r instanceof ItemStack && recipe.getRecipeOutput() != null && NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), (ItemStack) r)) {
							arecipes.add(new GergCachedRecipe(recipe));
							break;
						}
					}
				}
			}
		} else {
			super.loadCraftingRecipes(outputId, results);
		}
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		for(Object o : CraftingManager.getInstance().getRecipeList()) {
			if(o instanceof GergToolRecipe) {
				GergToolRecipe recipe = (GergToolRecipe) o;
				if(recipe.getRecipeOutput() != null && NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), result)) {
					arecipes.add(new GergCachedRecipe(recipe));
				}
			}
		}
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients) {
		if(inputId.equals("item") && getClass() == GergToolNEIHandler.class) {
			for(Object o : CraftingManager.getInstance().getRecipeList()) {
				if(o instanceof GergToolRecipe) {
					GergToolRecipe recipe = (GergToolRecipe) o;
					ItemStack[] display = recipe.getDisplayRecipe();
					if(display != null) {
						for(ItemStack slot : display) {
							if(slot == null) continue;
							for(Object ingredient : ingredients) {
								if(ingredient instanceof ItemStack && NEIServerUtils.areStacksSameTypeCrafting(slot, (ItemStack) ingredient)) {
									arecipes.add(new GergCachedRecipe(recipe));
									break;
								}
							}
						}
					}
				}
			}
		} else {
			super.loadUsageRecipes(inputId, ingredients);
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		for(Object o : CraftingManager.getInstance().getRecipeList()) {
			if(o instanceof GergToolRecipe) {
				GergToolRecipe recipe = (GergToolRecipe) o;
				ItemStack[] display = recipe.getDisplayRecipe();
				if(display != null) {
					for(ItemStack slot : display) {
						if(slot != null && NEIServerUtils.areStacksSameTypeCrafting(slot, ingredient)) {
							arecipes.add(new GergCachedRecipe(recipe));
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public int recipiesPerPage() {
		return 2;
	}

	private static boolean isAnyTool(ItemStack stack) {
		for(GergToolType type : GergToolType.values()) {
			if(GergToolType.isToolOfType(stack, type)) return true;
		}
		return false;
	}

	private class GergCachedRecipe extends CachedRecipe {

		private final List<PositionedStack> ingredients = new ArrayList<>();
		private PositionedStack result;

		public GergCachedRecipe(GergToolRecipe recipe) {
			ItemStack[] display = recipe.getDisplayRecipe();
			if(display != null) {
				for(int i = 0; i < 9; i++) {
					if(display[i] != null) {
						PositionedStack stack = new PositionedStack(display[i], 25 + (i % 3) * 18, 6 + (i / 3) * 18);
						if(isAnyTool(display[i])) {
							stack.setChance(0);
						}
						ingredients.add(stack);
					}
				}
			}
			this.result = new PositionedStack(recipe.getRecipeOutput(), 119, 24);
		}

		@Override
		public List<PositionedStack> getIngredients() {
			return getCycledIngredients(cycleticks / 48, ingredients);
		}

		@Override
		public PositionedStack getResult() {
			return result;
		}
	}
}
