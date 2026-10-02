package com.hbm.handler.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm.handler.imc.ICompatNHNEI;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;
import com.hbm.lib.RefStrings;
import com.hbm.util.i18n.I18nUtil;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import static codechicken.lib.gui.GuiDraw.drawTexturedModalRect;

/**
 * Overview page for the complete processing tree of a celestial bedrock ore.
 *
 * <p>Unlike the individual machine handlers, this handler owns one page per
 * {@link CelestialBedrockOreType}. Both recipe and usage lookup deliberately
 * lead to the same page so every intermediate grade can be used to enter the
 * processing diagram.</p>
 */
public class BedrockOreProcessingHandler extends TemplateRecipeHandler implements ICompatNHNEI {

	public static final String RECIPE_ID = "ntmBedrockOreProcessing";

	@Override
	public String getRecipeName() {
		return I18nUtil.resolveKey("nei.bedrock_ore_processing.name");
	}

	@Override
	public String getRecipeID() {
		return RECIPE_ID;
	}

	@Override
	public ItemStack[] getMachinesForRecipe() {
		return new ItemStack[] { new ItemStack(ModItems.bedrock_ore) };
	}

	@Override
	public String getGuiTexture() {
		return RefStrings.MODID + ":textures/gui/nei/gui_nei.png";
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if(RECIPE_ID.equals(outputId)) {
			addAllOreTypes();
		} else {
			super.loadCraftingRecipes(outputId, results);
		}
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		addRecipesFor(result);
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients) {
		if(RECIPE_ID.equals(inputId)) {
			addAllOreTypes();
		} else {
			super.loadUsageRecipes(inputId, ingredients);
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		addRecipesFor(ingredient);
	}

	private void addAllOreTypes() {
		for(CelestialBedrockOreType type : CelestialBedrockOre.getAllTypes()) {
			addRecipe(type);
		}
	}

	private void addRecipesFor(ItemStack stack) {
		if(stack == null) return;

		if(stack.getItem() == ModItems.bedrock_ore) {
			ItemBedrockOreNew item = (ItemBedrockOreNew) ModItems.bedrock_ore;
			addRecipe(item.getType(stack.getItemDamage()));
			return;
		}

		if(stack.getItem() == ModItems.bedrock_ore_base) {
			CelestialBedrockOre ore = CelestialBedrockOre.get(
					com.hbm.items.special.ItemBedrockOreBase.getOreBody(stack));
			if(ore == null) return;
			for(CelestialBedrockOreType type : ore.types) addRecipe(type);
		}
	}

	private void addRecipe(CelestialBedrockOreType type) {
		if(type != null) arecipes.add(new RecipeSet(type));
	}

	/**
	 * Draw lines and slot backgrounds behind the item stacks. Coordinates are
	 * relative to NEI's 166x65 recipe area.
	 */
	@Override
	public void drawBackground(int recipe) {
		super.drawBackground(recipe);
		drawDiagramBackground((RecipeSet) arecipes.get(recipe));
	}

	protected void drawDiagramBackground(RecipeSet recipe) {
		final int line = 0xFF606060;

		// Stem from the input, horizontal branch, and three downward arrows.
		Gui.drawRect(82, 20, 84, 34, line);
		Gui.drawRect(29, 33, 138, 35, line);
		for(int x : new int[] { 29, 83, 137 }) {
			Gui.drawRect(x - 1, 34, x + 1, 42, line);
			Gui.drawRect(x - 3, 39, x + 3, 41, line);
			Gui.drawRect(x - 2, 41, x + 2, 43, line);
		}

		// Gui.drawRect leaves the OpenGL color state set to the line color (0x606060), which tints
		// subsequent textures. Reset color back to full white before drawing slot textures.
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		// Slot texture used by the other Nuclear Tech NEI handlers.
		drawTexturedModalRect(73, 1, 5, 87, 18, 18);
		drawTexturedModalRect(19, 42, 5, 87, 18, 18);
		drawTexturedModalRect(73, 42, 5, 87, 18, 18);
		drawTexturedModalRect(127, 42, 5, 87, 18, 18);
	}

	@Override
	public void drawExtras(int recipe) {
		drawDiagramForeground((RecipeSet) arecipes.get(recipe));
	}

	/** Draw labels, overlays, and other foreground content here. */
	protected void drawDiagramForeground(RecipeSet recipe) {
		// Diagram labels intentionally left to the caller.
	}

	/**
	 * Add the diagram's item nodes here. These four nodes are a small working
	 * example; replace or extend them when implementing the complete tree.
	 */
	protected void buildDiagramStacks(RecipeSet recipe) {
		recipe.addIngredient(ItemBedrockOreNew.make(BedrockOreGrade.BASE, recipe.type), 74, 2);
		recipe.setResult(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY, recipe.type), 20, 43);
		recipe.addOtherStack(ItemBedrockOreNew.make(BedrockOreGrade.BASE_ROASTED, recipe.type), 74, 43);
		recipe.addOtherStack(ItemBedrockOreNew.make(BedrockOreGrade.BASE_WASHED, recipe.type), 128, 43);
	}

	public class RecipeSet extends TemplateRecipeHandler.CachedRecipe {

		public final CelestialBedrockOreType type;
		private final List<PositionedStack> ingredients = new ArrayList<>();
		private final List<PositionedStack> otherStacks = new ArrayList<>();
		private PositionedStack result;

		private RecipeSet(CelestialBedrockOreType type) {
			this.type = type;
			buildDiagramStacks(this);
		}

		public void addIngredient(Object stack, int x, int y) {
			ingredients.add(new PositionedStack(stack, x, y));
		}

		public void addOtherStack(Object stack, int x, int y) {
			otherStacks.add(new PositionedStack(stack, x, y));
		}

		public void setResult(Object stack, int x, int y) {
			result = new PositionedStack(stack, x, y);
		}

		@Override
		public List<PositionedStack> getIngredients() {
			return getCycledIngredients(cycleticks / 20, ingredients);
		}

		@Override
		public PositionedStack getResult() {
			return result;
		}

		@Override
		public List<PositionedStack> getOtherStacks() {
			if(otherStacks.isEmpty()) return Collections.emptyList();
			return getCycledIngredients(cycleticks / 20, otherStacks);
		}
	}
}
