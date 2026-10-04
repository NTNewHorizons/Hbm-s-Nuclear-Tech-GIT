package com.hbm.handler.nei;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.dim.SolarSystem;
import com.hbm.handler.imc.ICompatNHNEI;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;
import com.hbm.lib.RefStrings;
import com.hbm.util.i18n.I18nUtil;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import static codechicken.lib.gui.GuiDraw.drawTexturedModalRect;

/**
 * Complete celestial bedrock ore processing chart in NEI.
 * Displays the full sequential flowchart matching the InstantNootles bedrock chart:
 * - Base Ore Tier: Ore Slopper (Water) -> Base Ore -> Roasting (Pyro Oven + Vitriol) / Washing (Crystallizer + Water) -> Centrifuge -> Primary + Gravel
 * - Primary Ore Tier: Roasting (+ Vitriol), Direct Centrifuge (1x), Hydrogen Acidizing -> Electrolyser (8x), Arc Furnace (5x), Centrifuge (2x)
 * - Tier 1 (Sulfuric): Primary Ore -> Sulfuric Acid -> Centrifuge -> Primary NoSulfuric + Sulfuric Byproduct
 *   Acid Byproduct Upgrade Ladder: Roast (Pyro Oven + Vitriol) / Arc Smelt / Direct Wash -> Washed Byproduct -> Centrifuge / Arc Furnace -> Acid Fragment
 * - Tier 2 (Solvent): Primary NoSulfuric -> Solvent -> Centrifuge -> Primary NoSolvent + Solvent Byproduct
 *   Solvent Byproduct Upgrade Ladder: Roast / Arc / Wash -> Solvent Fragment
 * - Tier 3 (Radiosolvent): Primary NoSolvent -> Radiosolvent -> Centrifuge -> Primary NoRad + Rad Byproduct
 *   Primary NoRad -> Centrifuge -> Primary Fragment
 *   Rad Byproduct Upgrade Ladder: Roast / Arc / Wash -> Rad Fragment
 * - Bedrock Crumbs Recycling: Electrolyser / Arc Furnace / Crystallizer (Slop) -> Base Bedrock Ore
 */
public class BedrockOreProcessingHandler extends TemplateRecipeHandler implements ICompatNHNEI {

	public static final String RECIPE_ID = "ntmBedrockOreProcessing";
	public static final int CANVAS_WIDTH = 254;
	public static final int CANVAS_HEIGHT = 620;

	public static void injectHandlerInfo() {
		try {
			codechicken.nei.recipe.HandlerInfo info = new codechicken.nei.recipe.HandlerInfo(
				RECIPE_ID, RefStrings.NAME, RefStrings.MODID, false, null
			);
			info.setHandlerDimensions(CANVAS_WIDTH, CANVAS_HEIGHT);
			info.setAllowOverflowY(true);
			info.setAllowOverflowX(false);
			info.setMultipleWidgetsAllowed(false);
			info.setItem(RefStrings.MODID, "bedrock_ore");
			codechicken.nei.recipe.GuiRecipeTab.handlerMap.put(RECIPE_ID, info);
			codechicken.nei.recipe.GuiRecipeTab.handlerMap.put(BedrockOreProcessingHandler.class.getName(), info);
		} catch(Throwable ignored) { }
	}

	static {
		injectHandlerInfo();
		MinecraftForge.EVENT_BUS.register(new BedrockOreNEIListener());
	}

	public static class BedrockOreNEIListener {
		@SubscribeEvent
		public void onRegisterHandlerInfos(NEIRegisterHandlerInfosEvent event) {
			injectHandlerInfo();
			event.registerHandlerInfo(RECIPE_ID, RefStrings.NAME, RefStrings.MODID, b -> {
				b.setDisplayStack(new ItemStack(ModItems.bedrock_ore));
				b.setWidth(CANVAS_WIDTH);
				b.setHeight(CANVAS_HEIGHT);
				b.setAllowOverflowY(true);
				b.setAllowOverflowX(false);
				b.setMultipleWidgetsAllowed(false);
			});
			event.registerHandlerInfo(BedrockOreProcessingHandler.class, RefStrings.NAME, RefStrings.MODID, b -> {
				b.setDisplayStack(new ItemStack(ModItems.bedrock_ore));
				b.setWidth(CANVAS_WIDTH);
				b.setHeight(CANVAS_HEIGHT);
				b.setAllowOverflowY(true);
				b.setAllowOverflowX(false);
				b.setMultipleWidgetsAllowed(false);
			});
		}
	}

	@Override
	public String getOverlayIdentifier() {
		return RECIPE_ID;
	}

	@Override
	public String getHandlerId() {
		return RECIPE_ID;
	}

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
	public int getHandlerWidth() {
		return CANVAS_WIDTH;
	}

	@Override
	public int getHandlerHeight() {
		return CANVAS_HEIGHT;
	}

	@Override
	public int getMaxRecipesPerPage() {
		return 1;
	}

	@Override
	public boolean allowOverflowY() {
		return true;
	}

	@Override
	public int getRecipeHeight(int recipe) {
		return CANVAS_HEIGHT;
	}

	@Override
	public int recipiesPerPage() {
		return 1;
	}

	@Override
	public String getGuiTexture() {
		return RefStrings.MODID + ":textures/gui/nei/gui_nei.png";
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if(RECIPE_ID.equals(outputId) || "ntmBedrockOre".equals(outputId)) {
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
		if(RECIPE_ID.equals(inputId) || "ntmBedrockOre".equals(inputId)) {
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

		if(Block.getBlockFromItem(stack.getItem()) == ModBlocks.ore_bedrock) {
			addAllOreTypes();
			return;
		}

		if(stack.getItem() == ModItems.bedrock_ore) {
			ItemBedrockOreNew item = (ItemBedrockOreNew) ModItems.bedrock_ore;
			addRecipe(item.getType(stack.getItemDamage()));
			return;
		}

		if(stack.getItem() == ModItems.bedrock_ore_base) {
			CelestialBedrockOre ore = CelestialBedrockOre.get(ItemBedrockOreBase.getOreBody(stack));
			if(ore == null) return;
			for(CelestialBedrockOreType type : ore.types) addRecipe(type);
			return;
		}

		if(stack.getItem() == ModItems.bedrock_ore_fragment) {
			int damage = stack.getItemDamage();
			for(CelestialBedrockOreType type : CelestialBedrockOre.getAllTypes()) {
				if((type.primary != null && type.primary.mat.id == damage)
						|| (type.byproductAcid != null && type.byproductAcid.mat.id == damage)
						|| (type.byproductSolvent != null && type.byproductSolvent.mat.id == damage)
						|| (type.byproductRad != null && type.byproductRad.mat.id == damage)) {
					addRecipe(type);
				}
			}
		}
	}

	private void addRecipe(CelestialBedrockOreType type) {
		if(type != null) {
			for(CachedRecipe recipe : arecipes) {
				if(((RecipeSet) recipe).type == type) return;
			}
			arecipes.add(new RecipeSet(type));
		}
	}

	@Override
	public void drawBackground(int recipe) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		drawDiagramBackground((RecipeSet) arecipes.get(recipe));
	}

	private void drawDiagramBackground(RecipeSet recipe) {
		final int line = 0xFF606060;
		final int vitriolLine = 0xFFC09020;
		final int crumbLine = 0xFF505050;

		// ==================== SECTION 1: BASE ORE (y=18..110) ====================
		// Raw Ore Piece (127, 36) -> Ore Slopper (127, 40)
		drawVLine(127, 36, 40, line);
		drawDownArrow(127, 40, line);

		// Water Droplet (109, 48) -> Slopper (119, 48)
		drawHLine(109, 119, 48, line);
		drawRightArrow(119, 48, line);

		// Slopper (127, 56) -> Base Ore slot (127, 62)
		drawVLine(127, 56, 62, line);
		drawDownArrow(127, 62, line);

		// Roasting Branch (Left): Base Ore (118, 71) -> Pyro Oven (80, 63..79)
		drawHLine(96, 118, 71, line);
		drawLeftArrow(96, 71, line);

		// Pyro Oven (88, 63) -> Vitriol Droplet (88, 57)
		drawVLine(88, 57, 63, vitriolLine);
		drawUpArrow(88, 57, vitriolLine);

		// Pyro Oven (80, 71) -> Roasted Base Ore slot (66, 71)
		drawHLine(66, 80, 71, line);
		drawLeftArrow(66, 71, line);

		// Roasted Base Ore (57, 80) -> Line down to (57, 88) -> joins Centrifuge bus
		drawVLine(57, 80, 88, line);
		drawHLine(57, 119, 88, line);

		// Middle Branch: Base Ore (127, 80) -> Centrifuge (127, 88)
		drawVLine(127, 80, 88, line);
		drawDownArrow(127, 88, line);

		// Centrifuge (135, 96) -> Gravel Byproduct slot (152, 96)
		drawHLine(135, 152, 96, line);
		drawRightArrow(152, 96, line);

		// Washing Branch (Right): Base Ore (136, 71) -> Crystallizer (174, 71)
		drawHLine(136, 174, 71, line);
		drawRightArrow(174, 71, line);

		// Water Droplet (182, 57) -> Crystallizer (182, 63)
		drawVLine(182, 57, 63, line);
		drawDownArrow(182, 63, line);

		// Crystallizer (190, 71) -> Washed Base Ore slot (204, 71)
		drawHLine(190, 204, 71, line);
		drawRightArrow(204, 71, line);

		// Washed Base Ore (213, 80) -> Centrifuge x2 (213, 88)
		drawVLine(213, 80, 88, line);
		drawDownArrow(213, 88, line);

		// Centrifuges output down to bus at y=110, leading to Primary slot at (20, 116)
		drawVLine(127, 104, 110, line);
		drawVLine(213, 104, 110, line);
		drawHLine(29, 213, 110, line);
		drawVLine(29, 110, 116, line);
		drawDownArrow(29, 116, line);

		// ==================== SECTION 2: PRIMARY ORE PROCESSING (y=114..206) ====================
		// Main Vertical Acidization Bus runs down from (29, 134) to Tiers 1, 2, 3
		drawVLine(29, 134, 456, line);

		// Horizontal Primary Feed Bus at y=125
		drawHLine(29, 48, 125, line);

		// Roasting Branch (Top): (48, 125) -> (48, 124) -> Pyro Oven (58, 124)
		drawVLine(48, 124, 125, line);
		drawHLine(48, 58, 124, line);
		drawRightArrow(58, 124, line);

		// Pyro Oven (66, 116) -> Vitriol Droplet (66, 112)
		drawVLine(66, 112, 116, vitriolLine);
		drawUpArrow(66, 112, vitriolLine);

		// Pyro Oven (74, 124) -> Primary Roasted slot (88, 124)
		drawHLine(74, 88, 124, line);
		drawRightArrow(88, 124, line);

		// Roasted Primary (97, 134) loops down to join extraction bus at y=138
		drawVLine(97, 134, 138, line);
		drawHLine(48, 122, 138, line);

		// Direct Centrifuge Branch (Middle, 1x):
		drawHLine(48, 122, 132, line);
		drawRightArrow(122, 132, line);
		drawHLine(138, 154, 132, line);
		drawRightArrow(154, 132, line);

		// Hydrogen Acidizing Branch (Bottom): (48, 125) down to y=164 -> Crystallizer (58, 164)
		drawVLine(48, 125, 164, line);
		drawHLine(48, 58, 164, line);
		drawRightArrow(58, 164, line);

		// Hydrogen Droplet (66, 152) -> Crystallizer (66, 156)
		drawVLine(66, 152, 156, line);
		drawDownArrow(66, 156, line);

		// Crystallizer (74, 164) -> Primary First slot (88, 164)
		drawHLine(74, 88, 164, line);
		drawRightArrow(88, 164, line);

		// From Primary First (97, 164) to 3 High-Yield Extraction Methods:
		// 1. Electrolyser (8x): up to y=152 -> Electrolyser (122, 152)
		drawVLine(97, 152, 164, line);
		drawHLine(97, 122, 152, line);
		drawRightArrow(122, 152, line);
		drawHLine(138, 154, 152, line);
		drawRightArrow(154, 152, line);
		drawHLine(172, 182, 152, crumbLine);
		drawRightArrow(182, 152, crumbLine);

		// 2. Arc Furnace (5x Molten): straight to Arc Furnace (122, 174)
		drawVLine(97, 164, 174, line);
		drawHLine(97, 122, 174, line);
		drawRightArrow(122, 174, line);
		drawHLine(138, 154, 174, line);
		drawRightArrow(154, 174, line);

		// 3. Centrifuge (2x): down to y=196 -> Centrifuge (122, 196)
		drawVLine(97, 174, 196, line);
		drawHLine(97, 122, 196, line);
		drawRightArrow(122, 196, line);
		drawHLine(138, 154, 196, line);
		drawRightArrow(154, 196, line);
		drawHLine(172, 182, 196, crumbLine);
		drawRightArrow(182, 196, crumbLine);

		// ==================== SECTIONS 3, 4, 5: TIERS 1, 2, 3 ====================
		drawTierLines(line, vitriolLine, crumbLine, 216); // Sulfuric Acid
		drawTierLines(line, vitriolLine, crumbLine, 326); // Solvent
		drawTierLines(line, vitriolLine, crumbLine, 436); // Radiosolvent

		// ==================== SECTION 6: CRUMBS RECYCLING (y=546..612) ====================
		// Crumb collection trunk down along x=204 to Crumbs slot at (30, 566)
		drawVLine(204, 152, 540, crumbLine);
		drawHLine(39, 204, 540, crumbLine);
		drawVLine(39, 540, 566, crumbLine);
		drawDownArrow(39, 566, crumbLine);

		// Crumbs slot (39, 575) -> 3 Recycling Paths:
		// 1. Electrolyser (2x): up to y=556 -> Electrolyser (68, 556)
		drawVLine(39, 556, 566, line);
		drawHLine(39, 68, 556, line);
		drawRightArrow(68, 556, line);
		drawHLine(84, 98, 556, line);
		drawRightArrow(98, 556, line);

		// 2. Arc Furnace (1x): straight to Arc Furnace (68, 579)
		drawHLine(48, 68, 579, line);
		drawRightArrow(68, 579, line);
		drawHLine(84, 98, 579, line);
		drawRightArrow(98, 579, line);

		// 3. Crystallizer (Slop Reform): down to y=602 -> Crystallizer (68, 602)
		drawVLine(39, 584, 602, line);
		drawHLine(39, 68, 602, line);
		drawRightArrow(68, 602, line);

		// Slop Droplet (76, 590) -> Crystallizer (76, 594)
		drawVLine(76, 590, 594, line);
		drawDownArrow(76, 594, line);

		// Crystallizer (84, 602) -> Base Bedrock Ore slot (98, 602)
		drawHLine(84, 98, 602, line);
		drawRightArrow(98, 602, line);

		// Draw Slots using clean light slot texture
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GuiDraw.changeTexture("hbm:textures/gui/nei/bedrock_tree_slots.png");

		// Large starting slot for raw ore piece (26x26)
		drawTexturedModalRect(114, 14, 18, 0, 26, 26);

		// Regular 18x18 slots
		for(Point slot : recipe.slotPositions) {
			drawTexturedModalRect(slot.x - 1, slot.y - 1, 0, 0, 18, 18);
		}
	}

	private static void drawTierLines(int line, int vitriolLine, int crumbLine, int tierY) {
		// Feed from main bus at x=29 into Crystallizer at (44, tierY + 20)
		drawHLine(29, 44, tierY + 20, line);
		drawRightArrow(44, tierY + 20, line);

		// Reagent Droplet (52, tierY + 8) -> Crystallizer (52, tierY + 12)
		drawVLine(52, tierY + 8, tierY + 12, line);
		drawDownArrow(52, tierY + 12, line);

		// Crystallizer (60, tierY + 20) -> Leached Chunk slot (74, tierY + 20)
		drawHLine(60, 74, tierY + 20, line);
		drawRightArrow(74, tierY + 20, line);

		// Leached Chunk (92, tierY + 20) -> Centrifuge (100, tierY + 20)
		drawHLine(92, 100, tierY + 20, line);
		drawRightArrow(100, tierY + 20, line);

		// Top Output: Cleaned Chunk
		drawVLine(108, tierY + 3, tierY + 12, line);
		drawHLine(108, 124, tierY + 3, line);
		drawRightArrow(124, tierY + 3, line);

		// Cleaned Chunk -> Centrifuge -> Primary Fragment + Crumbs
		drawHLine(142, 148, tierY + 3, line);
		drawRightArrow(148, tierY + 3, line);
		drawHLine(164, 172, tierY + 3, line);
		drawRightArrow(172, tierY + 3, line);
		drawHLine(190, 194, tierY + 3, crumbLine);
		drawRightArrow(194, tierY + 3, crumbLine);

		// Cleaned Chunk also sends line down to feed next tier
		drawVLine(133, tierY + 12, tierY + 28, line);
		drawHLine(29, 133, tierY + 28, line);

		// Bottom Output: Byproduct Chunk
		drawVLine(108, tierY + 28, tierY + 38, line);
		drawHLine(108, 124, tierY + 38, line);
		drawRightArrow(124, tierY + 38, line);

		// Byproduct Upgrade Ladder (3 Branches):
		// Branch A (Direct Wash, 1x): straight to Crystallizer (176, tierY + 38)
		drawHLine(142, 176, tierY + 38, line);
		drawRightArrow(176, tierY + 38, line);

		// Branch B (Arc Smelt, 2x): down to y=tierY + 58 -> Arc Furnace (148, tierY + 58)
		drawVLine(133, tierY + 47, tierY + 58, line);
		drawHLine(133, 148, tierY + 58, line);
		drawRightArrow(148, tierY + 58, line);
		drawHLine(164, 172, tierY + 58, line);
		drawRightArrow(172, tierY + 58, line);
		// Arc Chunk line goes up to enter Crystallizer (Wash)
		drawVLine(181, tierY + 44, tierY + 49, line);
		drawHLine(181, 184, tierY + 44, line);

		// Branch C (Roast + Arc Smelt, 4x): down to y=tierY + 80 -> Pyro Oven (148, tierY + 80)
		drawVLine(133, tierY + 58, tierY + 80, line);
		drawHLine(133, 148, tierY + 80, line);
		drawRightArrow(148, tierY + 80, line);

		// Pyro Oven -> Vitriol Droplet below
		drawVLine(156, tierY + 88, tierY + 92, vitriolLine);
		drawDownArrow(156, tierY + 92, vitriolLine);

		// Pyro Oven -> Roasted Byproduct slot -> Arc Furnace x4 -> Arc Byproduct x4
		drawHLine(164, 172, tierY + 80, line);
		drawRightArrow(172, tierY + 80, line);
		drawHLine(190, 198, tierY + 80, line);
		drawRightArrow(198, tierY + 80, line);
		drawHLine(214, 222, tierY + 80, line);
		drawRightArrow(222, tierY + 80, line);

		// Arc Byproduct x4 line goes up to enter Crystallizer (Wash)
		drawVLine(231, tierY + 38, tierY + 71, line);
		drawHLine(192, 231, tierY + 38, line);
		drawLeftArrow(192, tierY + 38, line);

		// Water Droplet (184, tierY + 26) -> Crystallizer (184, tierY + 30)
		drawVLine(184, tierY + 26, tierY + 30, line);
		drawDownArrow(184, tierY + 30, line);

		// Crystallizer (192, tierY + 38) -> Washed Byproduct slot (204, tierY + 38)
		drawHLine(192, 204, tierY + 38, line);
		drawRightArrow(204, tierY + 38, line);

		// From Washed Byproduct (213, tierY + 38) to 2 Extractions:
		// 1. Centrifuge (Extract Fragment):
		drawVLine(213, tierY + 18, tierY + 29, line);
		drawHLine(213, 220, tierY + 18, line);
		drawRightArrow(220, tierY + 18, line);
		drawHLine(236, 234, tierY + 18, line);
		drawRightArrow(234, tierY + 18, line);

		// 2. Arc Furnace (Smelt to Fluid):
		drawVLine(213, tierY + 47, tierY + 48, line);
		drawHLine(213, 220, tierY + 48, line);
		drawRightArrow(220, tierY + 48, line);
		drawHLine(236, 234, tierY + 48, line);
		drawRightArrow(234, tierY + 48, line);
	}

	private static void drawHLine(int x1, int x2, int y, int color) {
		int min = Math.min(x1, x2);
		int max = Math.max(x1, x2);
		Gui.drawRect(min, y - 1, max, y + 1, color);
	}

	private static void drawVLine(int x, int y1, int y2, int color) {
		int min = Math.min(y1, y2);
		int max = Math.max(y1, y2);
		Gui.drawRect(x - 1, min, x + 1, max, color);
	}

	private static void drawDownArrow(int x, int y, int color) {
		Gui.drawRect(x - 3, y - 4, x + 3, y - 2, color);
		Gui.drawRect(x - 2, y - 2, x + 2, y - 1, color);
		Gui.drawRect(x - 1, y - 1, x + 1, y, color);
	}

	private static void drawUpArrow(int x, int y, int color) {
		Gui.drawRect(x - 3, y + 2, x + 3, y + 4, color);
		Gui.drawRect(x - 2, y + 1, x + 2, y + 2, color);
		Gui.drawRect(x - 1, y, x + 1, y + 1, color);
	}

	private static void drawLeftArrow(int x, int y, int color) {
		Gui.drawRect(x + 2, y - 3, x + 4, y + 3, color);
		Gui.drawRect(x + 1, y - 2, x + 2, y + 2, color);
		Gui.drawRect(x, y - 1, x + 1, y + 1, color);
	}

	private static void drawRightArrow(int x, int y, int color) {
		Gui.drawRect(x - 4, y - 3, x - 2, y + 3, color);
		Gui.drawRect(x - 2, y - 2, x - 1, y + 2, color);
		Gui.drawRect(x - 1, y - 1, x, y + 1, color);
	}

	@Override
	public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
		if(button == 0) {
			Point mouse = GuiDraw.getMousePosition();
			Point offset = gui.getRecipePosition(recipe);
			int relX = mouse.x - offset.x;
			int relY = mouse.y - offset.y;

			RecipeSet set = (RecipeSet) arecipes.get(recipe);
			for(ClickableMachine m : set.machines) {
				if(relX >= m.x && relX < m.x + 16 && relY >= m.y && relY < m.y + 16) {
					GuiCraftingRecipe.openRecipeGui(m.recipeId);
					return true;
				}
			}

			// Vitriol droplets open Pyro Oven recipes
			if((relX >= 76 && relX < 96 && relY >= 35 && relY < 55)
					|| (relX >= 56 && relX < 76 && relY >= 90 && relY < 110)
					|| (relX >= 144 && relX < 164 && (relY >= 300 && relY < 325 || relY >= 410 && relY < 435 || relY >= 520 && relY < 545))) {
				GuiCraftingRecipe.openRecipeGui("ntmPyrolysis");
				return true;
			}
		}
		return super.mouseClicked(gui, button, recipe);
	}

	@Override
	public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
		currenttip = super.handleTooltip(gui, currenttip, recipe);
		Point mouse = GuiDraw.getMousePosition();
		Point offset = gui.getRecipePosition(recipe);
		int relX = mouse.x - offset.x;
		int relY = mouse.y - offset.y;

		RecipeSet set = (RecipeSet) arecipes.get(recipe);
		for(ClickableMachine m : set.machines) {
			if(relX >= m.x && relX < m.x + 16 && relY >= m.y && relY < m.y + 16) {
				currenttip.add(EnumChatFormatting.GREEN + m.name);
				if(m.description != null && !m.description.isEmpty()) {
					currenttip.add(EnumChatFormatting.GRAY + m.description);
				}
				currenttip.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("nei.bedrock.click_recipes"));
				return currenttip;
			}
		}

		// Vitriol tooltip
		if((relX >= 76 && relX < 96 && relY >= 35 && relY < 55)
				|| (relX >= 56 && relX < 76 && relY >= 90 && relY < 110)
				|| (relX >= 144 && relX < 164 && (relY >= 300 && relY < 325 || relY >= 410 && relY < 435 || relY >= 520 && relY < 545))) {
			currenttip.add(EnumChatFormatting.GOLD + "Vitriol (50 mB)");
			currenttip.add(EnumChatFormatting.GRAY + "Byproduct from roasting ore chunks in Pyro Oven");
			currenttip.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("nei.bedrock.click_recipes"));
			return currenttip;
		}

		return currenttip;
	}

	public static String formatSuffix(String suffix) {
		if(suffix == null) return "Bedrock Ore";
		switch(suffix.toLowerCase(java.util.Locale.US)) {
			case "light": return "Light Metal";
			case "heavy": return "Heavy Metal";
			case "rare": return "Rare Earth";
			case "actinide": return "Actinide";
			case "nonmetal": return "Non-Metal";
			case "crystal": return "Crystalline";
			case "schrabidic": return "Schrabidic";
			case "hazard": return "Hazardous";
			case "plastic": return "Plastic";
			default: return Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1);
		}
	}

	private static void drawBadge(String text, int x, int y, int textColor, int bgColor) {
		int w = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
		Gui.drawRect(x - 2, y - 2, x + w + 2, y + 9, 0xFF454545);
		Gui.drawRect(x - 1, y - 1, x + w + 1, y + 8, bgColor);
		GuiDraw.drawString(text, x, y, textColor, false);
	}

	@Override
	public void drawExtras(int recipe) {
		RecipeSet set = (RecipeSet) arecipes.get(recipe);

		String bodyName = set.type.body != null ? I18nUtil.resolveKey("body." + set.type.body.name) : "Space";
		String typeName = formatSuffix(set.type.suffix);
		String title = " " + typeName + " (" + bodyName + ") ";
		drawBadge(title, 8, 4, 0xFFFFFF, 0xFF252525);

		// Section headers / annotations
		drawBadge(" Base Ore ", 8, 66, 0xFFFFFF, 0xFF353535);
		drawBadge(" Primary Ore ", 8, 102, 0xFFFFFF, 0xFF353535);
		drawBadge(" Tier 1: Sulfuric ", 8, 202, 0xFFFF88, 0xFF353535);
		drawBadge(" Tier 2: Solvent ", 8, 312, 0x88FFFF, 0xFF353535);
		drawBadge(" Tier 3: Radiosolvent ", 8, 422, 0x88FF88, 0xFF353535);
		drawBadge(" Crumbs Recycling ", 8, 532, 0xFFAAAA, 0xFF353535);

		// Multipliers
		drawBadge("x2", 221, 91, 0xFFD700, 0xFF181818);
		drawBadge("1x", 137, 127, 0xE0E0E0, 0xFF181818);
		drawBadge("8x", 137, 147, 0x55FF55, 0xFF181818);
		drawBadge("5x", 137, 169, 0x55FF55, 0xFF181818);
		drawBadge("2x", 137, 191, 0xE0E0E0, 0xFF181818);

		// Tier multipliers
		for(int tierY : new int[] { 216, 326, 436 }) {
			drawBadge("x2", 107, tierY + 4, 0xFFD700, 0xFF181818);
			drawBadge("2x", 155, tierY + 42, 0xFFD700, 0xFF181818);
			drawBadge("4x", 205, tierY + 64, 0x55FF55, 0xFF181818);
		}

		// Crumbs recycling multipliers
		drawBadge("2x", 83, 551, 0xFFD700, 0xFF181818);
		drawBadge("1x", 83, 574, 0xE0E0E0, 0xFF181818);
		drawBadge("x64", 69, 607, 0xFF5555, 0xFF181818);
	}

	public static class ClickableMachine {
		public int x, y;
		public String recipeId;
		public String name;
		public String description;

		public ClickableMachine(int x, int y, String recipeId, String name, String description) {
			this.x = x;
			this.y = y;
			this.recipeId = recipeId;
			this.name = name;
			this.description = description;
		}
	}

	public class RecipeSet extends CachedRecipe {
		public CelestialBedrockOreType type;
		public List<PositionedStack> ingredients = new ArrayList<>();
		public List<PositionedStack> otherStacks = new ArrayList<>();
		public List<Point> slotPositions = new ArrayList<>();
		public List<ClickableMachine> machines = new ArrayList<>();
		public PositionedStack result;

		public RecipeSet(CelestialBedrockOreType type) {
			this.type = type;

			// ==================== SECTION 1: BASE ORE (y=18..110) ====================
			// Starting raw ore chunk / block
			addSlot(118, 18);
			ItemStack rawOre = type.body != null ? new ItemStack(ModItems.bedrock_ore_base, 1, type.body.ordinal()) : new ItemStack(ModBlocks.ore_bedrock);
			ingredients.add(new PositionedStack(rawOre, 118, 18));

			// Ore Slopper (119, 40) + Water (93, 40)
			machines.add(new ClickableMachine(119, 40, "ntmOreSlopper", "Ore Slopper", "Consumes 1000mB Water to wash ore chunk into Base Bedrock Ore"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_ore_slopper), 119, 40));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.WATER, 1000), 93, 40));

			// Base Bedrock Ore
			addSlot(118, 62);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.BASE, type), 118, 62));

			// Roasting Branch: Pyro Oven (80, 63) + Vitriol (80, 41) -> Roasted Base Ore (48, 62)
			machines.add(new ClickableMachine(80, 63, "ntmPyrolysis", "Pyro Oven (Combination Oven)", "Roasts Base Ore into Roasted Base Ore. Outputs 50mB Vitriol"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_pyrooven), 80, 63));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.VITRIOL, 50), 80, 41));

			addSlot(48, 62);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.BASE_ROASTED, type), 48, 62));

			// Direct Centrifuge Branch: Centrifuge (119, 88) -> Gravel (152, 87)
			machines.add(new ClickableMachine(119, 88, "ntmCentrifuge", "Centrifuge", "Centrifuges Base Ore into 1x Primary Ore chunk + Gravel"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 119, 88));

			addSlot(152, 87);
			otherStacks.add(new PositionedStack(new ItemStack(Blocks.gravel), 152, 87));

			// Washing Branch: Crystallizer (174, 63) + Water (174, 41) -> Washed Base Ore (204, 62)
			machines.add(new ClickableMachine(174, 63, "ntmCrystallizer", "Crystallizer (Washing)", "Washes Base Ore with 250mB Water into Washed Base Ore"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_crystallizer), 174, 63));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.WATER, 250), 174, 41));

			addSlot(204, 62);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.BASE_WASHED, type), 204, 62));

			// Centrifuge x2 (205, 88)
			machines.add(new ClickableMachine(205, 88, "ntmCentrifuge", "Centrifuge (Double Yield)", "Centrifuges Washed Base Ore into 2x Primary Ore chunks + Gravel"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 205, 88));

			// ==================== SECTION 2: PRIMARY ORE PROCESSING (y=114..206) ====================
			// Primary Bedrock Ore chunk slot
			addSlot(20, 116);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY, type, 2), 20, 116));

			// 1. Roasting: Pyro Oven (58, 116) + Vitriol (58, 96) -> Roasted Primary (88, 116)
			machines.add(new ClickableMachine(58, 116, "ntmPyrolysis", "Pyro Oven (Combination Oven)", "Roasts Primary Ore into Roasted Primary Ore. Outputs 50mB Vitriol"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_pyrooven), 58, 116));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.VITRIOL, 50), 58, 96));

			addSlot(88, 116);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_ROASTED, type), 88, 116));

			// 2. Direct Centrifuge (1x yield): Centrifuge (122, 124) -> Primary Fragment (154, 123)
			machines.add(new ClickableMachine(122, 124, "ntmCentrifuge", "Centrifuge (Extract Primary)", "Centrifuges Primary Ore into 1x Primary Ore fragment"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 122, 124));

			addSlot(154, 123);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 1), 154, 123));

			// 3. Hydrogen Leaching (High Yield): Crystallizer (58, 156) + Hydrogen (58, 136) -> Primary First (88, 155)
			machines.add(new ClickableMachine(58, 156, "ntmCrystallizer", "Crystallizer (Hydrogen Leaching)", "Leaches Primary Ore with 250mB Hydrogen to produce Primary First chunk"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_crystallizer), 58, 156));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.HYDROGEN, 250), 58, 136));

			addSlot(88, 155);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_FIRST, type), 88, 155));

			// Electrolyser (8x Yield!): Electrolyser (122, 144) -> Primary Fragment 8x (154, 144) + Crumbs (182, 144)
			machines.add(new ClickableMachine(122, 144, "ntmElectrolysisMetal", "Electrolyser (High Yield)", "Electrolyzes Primary First chunk into 8x Primary Ore + Bedrock Crumbs!"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_electrolyser), 122, 144));

			addSlot(154, 144);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 8), 154, 144));
			addSlot(182, 144);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.CRUMBS, type), 182, 144));

			// Arc Furnace (5 Ingots Molten!): Arc Furnace (122, 166) -> Primary Output (154, 165)
			machines.add(new ClickableMachine(122, 166, "ntmArcFurnaceSolid", "Arc Furnace (Smelt to Fluid)", "Smelts Primary First chunk directly into 5 ingots worth of molten metal"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_arc_furnace), 122, 166));

			addSlot(154, 165);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 5), 154, 165));

			// Centrifuge (2x Yield): Centrifuge (122, 188) -> Primary Fragment 2x (154, 187) + Crumbs (182, 187)
			machines.add(new ClickableMachine(122, 188, "ntmCentrifuge", "Centrifuge (Double Yield)", "Centrifuges Primary First chunk into 2x Primary Ore + Bedrock Crumbs"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 122, 188));

			addSlot(154, 187);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 2), 154, 187));
			addSlot(182, 187);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.CRUMBS, type), 182, 187));

			// ==================== SECTIONS 3, 4, 5: TIERS 1, 2, 3 ====================
			buildTier(216, Fluids.SULFURIC_ACID, "Sulfuric Acid",
					BedrockOreGrade.PRIMARY_SULFURIC, BedrockOreGrade.PRIMARY_NOSULFURIC,
					BedrockOreGrade.SULFURIC_BYPRODUCT, BedrockOreGrade.SULFURIC_ROASTED,
					BedrockOreGrade.SULFURIC_ARC, BedrockOreGrade.SULFURIC_WASHED,
					ItemBedrockOreNew.extract(type.byproductAcid, 1));

			buildTier(326, Fluids.SOLVENT, "Solvent",
					BedrockOreGrade.PRIMARY_SOLVENT, BedrockOreGrade.PRIMARY_NOSOLVENT,
					BedrockOreGrade.SOLVENT_BYPRODUCT, BedrockOreGrade.SOLVENT_ROASTED,
					BedrockOreGrade.SOLVENT_ARC, BedrockOreGrade.SOLVENT_WASHED,
					ItemBedrockOreNew.extract(type.byproductSolvent, 1));

			buildTier(436, Fluids.RADIOSOLVENT, "Radiosolvent",
					BedrockOreGrade.PRIMARY_RAD, BedrockOreGrade.PRIMARY_NORAD,
					BedrockOreGrade.RAD_BYPRODUCT, BedrockOreGrade.RAD_ROASTED,
					BedrockOreGrade.RAD_ARC, BedrockOreGrade.RAD_WASHED,
					ItemBedrockOreNew.extract(type.byproductRad, 1));

			// ==================== SECTION 6: CRUMBS RECYCLING (y=546..612) ====================
			addSlot(30, 566);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.CRUMBS, type), 30, 566));

			// Electrolyser (2x)
			machines.add(new ClickableMachine(68, 548, "ntmElectrolysisMetal", "Electrolyser (Recycle Crumbs)", "Electrolyzes Bedrock Crumbs into 2x Primary Ore"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_electrolyser), 68, 548));

			addSlot(98, 547);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 2), 98, 547));

			// Arc Furnace (1 Ingot)
			machines.add(new ClickableMachine(68, 571, "ntmArcFurnaceSolid", "Arc Furnace (Smelt Crumbs)", "Smelts Bedrock Crumbs into 1 ingot worth of molten metal"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_arc_furnace), 68, 571));

			addSlot(98, 570);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 1), 98, 570));

			// Crystallizer (Reconstruction with Slop)
			machines.add(new ClickableMachine(68, 594, "ntmCrystallizer", "Crystallizer (Reform Crumbs)", "Recombines 64 Bedrock Crumbs + 1000mB Slop into Base Bedrock Ore!"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_crystallizer), 68, 594));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.SLOP, 1000), 68, 574));

			addSlot(98, 593);
			setResult(ItemBedrockOreNew.make(BedrockOreGrade.BASE, type), 98, 593);
		}

		private void buildTier(int tierY, com.hbm.inventory.fluid.FluidType fluid, String fluidName,
							   BedrockOreGrade leachedGrade, BedrockOreGrade cleanedGrade,
							   BedrockOreGrade bypGrade, BedrockOreGrade roastedGrade,
							   BedrockOreGrade arcGrade, BedrockOreGrade washedGrade,
							   ItemStack finalFragment) {

			// Crystallizer for Acidization + Fluid Droplet (centered directly above)
			machines.add(new ClickableMachine(44, tierY + 12, "ntmCrystallizer", "Crystallizer (" + fluidName + " Leaching)", "Leaches ore with 250mB " + fluidName));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_crystallizer), 44, tierY + 12));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(fluid, 250), 44, tierY - 8));

			// Leached Chunk
			addSlot(74, tierY + 11);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(leachedGrade, type), 74, tierY + 11));

			// Centrifuge for Separation
			machines.add(new ClickableMachine(100, tierY + 12, "ntmCentrifuge", "Centrifuge (Separation)", "Separates leached ore into Cleaned Ore (2x) + Byproduct Ore (2x)"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 100, tierY + 12));

			// Cleaned Chunk
			addSlot(124, tierY - 6);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(cleanedGrade, type, 2), 124, tierY - 6));

			// Cleaned Chunk -> Centrifuge -> Primary Fragment + Crumbs
			machines.add(new ClickableMachine(148, tierY - 5, "ntmCentrifuge", "Centrifuge (Extract Primary)", "Centrifuges Cleaned Ore into 1x Primary Ore + Bedrock Crumbs"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 148, tierY - 5));

			addSlot(172, tierY - 6);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.extract(type.primary, 1), 172, tierY - 6));
			addSlot(194, tierY - 6);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(BedrockOreGrade.CRUMBS, type), 194, tierY - 6));

			// Byproduct Chunk
			addSlot(124, tierY + 29);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(bypGrade, type, 2), 124, tierY + 29));

			// Branch B: Arc Furnace (x2) -> Arc Byproduct (x2)
			machines.add(new ClickableMachine(148, tierY + 50, "ntmArcFurnaceSolid", "Arc Furnace (x2 Smelt)", "Smelts Byproduct Ore into 2x Arc Byproduct"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_arc_furnace), 148, tierY + 50));

			addSlot(172, tierY + 49);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(arcGrade, type, 2), 172, tierY + 49));

			// Branch C: Pyro Oven (Roast) + Vitriol -> Roasted Byproduct
			machines.add(new ClickableMachine(148, tierY + 72, "ntmPyrolysis", "Pyro Oven (Roast Byproduct)", "Roasts Byproduct Ore into Roasted Byproduct. Outputs 50mB Vitriol"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_pyrooven), 148, tierY + 72));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.VITRIOL, 50), 148, tierY + 92));

			addSlot(172, tierY + 71);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(roastedGrade, type), 172, tierY + 71));

			// Arc Furnace (x4) -> Arc Byproduct (x4)
			machines.add(new ClickableMachine(198, tierY + 72, "ntmArcFurnaceSolid", "Arc Furnace (x4 Smelt)", "Smelts Roasted Byproduct into 4x Arc Byproduct"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_arc_furnace), 198, tierY + 72));

			addSlot(222, tierY + 71);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(arcGrade, type, 4), 222, tierY + 71));

			// Crystallizer (Wash) + Water (centered directly above)
			machines.add(new ClickableMachine(176, tierY + 30, "ntmCrystallizer", "Crystallizer (Byproduct Wash)", "Washes byproduct ore with 250mB Water into Washed Byproduct"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_crystallizer), 176, tierY + 30));
			otherStacks.add(new PositionedStack(ItemFluidIcon.make(Fluids.WATER, 250), 176, tierY + 10));

			addSlot(204, tierY + 29);
			otherStacks.add(new PositionedStack(ItemBedrockOreNew.make(washedGrade, type), 204, tierY + 29));

			// Final Extraction 1: Centrifuge -> Final Byproduct Fragment + Crumbs
			machines.add(new ClickableMachine(220, tierY + 10, "ntmCentrifuge", "Centrifuge (Extract Fragment)", "Centrifuges Washed Byproduct into Final Byproduct Fragment + Crumbs"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_centrifuge), 220, tierY + 10));

			addSlot(234, tierY + 9);
			otherStacks.add(new PositionedStack(finalFragment, 234, tierY + 9));

			// Final Extraction 2: Arc Furnace -> Molten Byproduct (3 Ingots)
			machines.add(new ClickableMachine(220, tierY + 40, "ntmArcFurnaceSolid", "Arc Furnace (Smelt Byproduct)", "Smelts Washed Byproduct into 3 ingots worth of molten metal"));
			otherStacks.add(new PositionedStack(new ItemStack(ModBlocks.machine_arc_furnace), 220, tierY + 40));

			addSlot(234, tierY + 39);
			otherStacks.add(new PositionedStack(finalFragment, 234, tierY + 39));
		}

		private void addSlot(int x, int y) {
			slotPositions.add(new Point(x, y));
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
			return otherStacks;
		}
	}
}
