package com.hbm.compat.neicustomdiagram;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.hbm.items.machine.ItemFluidIcon;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Grid;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Grid.Direction;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Layout;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Lines;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Slot;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.ComponentDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.draw.Draw;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;
import com.hbm.lib.RefStrings;
import com.hbm.util.i18n.I18nUtil;

import codechicken.nei.api.API;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Custom NEI Diagram for Celestial Bedrock Ore Processing.
 * Renders the complete, unsplit processing tree from raw ore piece to fragments
 * with interactive nodes, fluid requirements, machine links, and vertical scrolling.
 */
public class BedrockOreDiagramHandler {

	public static final Layout.SlotKey RAW_ORE = Layout.SlotKey.create("raw_ore");
	public static final Layout.SlotKey BASE_ROASTED = Layout.SlotKey.create("base_roasted");
	public static final Layout.SlotKey BASE = Layout.SlotKey.create("base");
	public static final Layout.SlotKey BASE_WASHED = Layout.SlotKey.create("base_washed");
	public static final Layout.SlotKey PRIMARY = Layout.SlotKey.create("primary");
	public static final Layout.SlotKey PRIMARY_SULFURIC = Layout.SlotKey.create("primary_sulfuric");
	public static final Layout.SlotKey PRIMARY_SOLVENT = Layout.SlotKey.create("primary_solvent");
	public static final Layout.SlotKey PRIMARY_RAD = Layout.SlotKey.create("primary_rad");
	public static final Layout.SlotKey PRIMARY_CLEANED = Layout.SlotKey.create("primary_cleaned");
	public static final Layout.SlotKey SULFURIC_BYPRODUCT = Layout.SlotKey.create("sulfuric_byproduct");
	public static final Layout.SlotKey SOLVENT_BYPRODUCT = Layout.SlotKey.create("solvent_byproduct");
	public static final Layout.SlotKey RAD_BYPRODUCT = Layout.SlotKey.create("rad_byproduct");
	public static final Layout.SlotKey SULFURIC_WASHED = Layout.SlotKey.create("sulfuric_washed");
	public static final Layout.SlotKey SOLVENT_WASHED = Layout.SlotKey.create("solvent_washed");
	public static final Layout.SlotKey RAD_WASHED = Layout.SlotKey.create("rad_washed");
	public static final Layout.SlotKey FRAGMENT_PRIMARY = Layout.SlotKey.create("fragment_primary");
	public static final Layout.SlotKey FRAGMENT_ACID = Layout.SlotKey.create("fragment_acid");
	public static final Layout.SlotKey FRAGMENT_SOLVENT = Layout.SlotKey.create("fragment_solvent");
	public static final Layout.SlotKey FRAGMENT_RAD = Layout.SlotKey.create("fragment_rad");
	public static final Layout.SlotKey CRUMBS = Layout.SlotKey.create("crumbs");

	public static void register() {
		DiagramGroupInfo info = DiagramGroupInfo.builder(
				I18nUtil.resolveKey("nei.bedrock_ore_processing.name"),
				"ntm.bedrock_ore_processing",
				ItemComponent.create(new ItemStack(ModItems.bedrock_ore)),
				1)
				.setAllowOverflowY(true)
				.setAllowOverflowX(false)
				.setWidth(Grid.TOTAL_WIDTH)
				.setDescription("Displays the complete processing tree from celestial bedrock ore pieces to fragments.")
				.build();

		MinecraftForge.EVENT_BUS.register(new HandlerInfoListener(info));

		Layout baseLayout = buildBaseLayout();
		ComponentDiagramMatcher.Builder matcherBuilder = ComponentDiagramMatcher.builder();

		for(CelestialBedrockOreType type : CelestialBedrockOre.getAllTypes()) {
			Diagram diagram = buildDiagramForType(baseLayout, type);
			ComponentDiagramMatcher.Builder.DiagramSubBuilder subBuilder = matcherBuilder.addDiagram(diagram);

			// Index raw ore piece for recipe/usage lookup
			ItemStack rawPiece = new ItemStack(ModItems.bedrock_ore_base, 1, type.body.ordinal());
			subBuilder.addComponent(ItemComponent.create(rawPiece));
			if(type.body == com.hbm.dim.SolarSystem.Body.KERBIN) {
				subBuilder.addComponent(ItemComponent.create(new ItemStack(ModItems.bedrock_ore_base, 1, 0)));
			}

			// Index all intermediate bedrock ore chunk grades
			for(BedrockOreGrade grade : BedrockOreGrade.values()) {
				subBuilder.addComponent(ItemComponent.create(ItemBedrockOreNew.make(grade, type)));
			}

			// Index all 4 final output fragments
			subBuilder.addComponent(ItemComponent.create(ItemBedrockOreNew.extract(type.primary, 1)));
			subBuilder.addComponent(ItemComponent.create(ItemBedrockOreNew.extract(type.byproductAcid, 1)));
			subBuilder.addComponent(ItemComponent.create(ItemBedrockOreNew.extract(type.byproductSolvent, 1)));
			subBuilder.addComponent(ItemComponent.create(ItemBedrockOreNew.extract(type.byproductRad, 1)));
		}

		DiagramGroup diagramGroup = new DiagramGroup(info, matcherBuilder.build());
		API.registerRecipeHandler(diagramGroup);
		API.registerUsageHandler(diagramGroup);
	}

	private static Layout buildBaseLayout() {
		Grid g = Grid.GRID;
		Layout.Builder builder = Layout.builder().setPaddingBottom(12);

		// Slots Definition
		// Row 0 (y=0): Raw Ore Piece (Large 26x26 Slot)
		builder.putSlot(RAW_ORE, Slot.builder(g.grid(6, 0))
				.setSlotWidth(Grid.BIG_SLOT_WIDTH)
				.setDrawFunction(Draw::drawBigSlot)
				.setTooltip(Tooltip.create("Raw Celestial Ore Piece\nMined from celestial bedrock ore bodies"))
				.build());

		// Row 1 (y=2): Base Ore Tier
		builder.putSlot(BASE_ROASTED, Slot.builder(g.grid(2, 2))
				.setTooltip(Tooltip.create("Roasted Base Ore\nCombination / Pyro Oven (+50 mB Vitriol)"))
				.build());
		builder.putSlot(BASE, Slot.builder(g.grid(6, 2))
				.setTooltip(Tooltip.create("Base Bedrock Ore\nFrom Ore Slopper with Water"))
				.build());
		builder.putSlot(BASE_WASHED, Slot.builder(g.grid(10, 2))
				.setTooltip(Tooltip.create("Washed Base Ore\nCrystallizer with Water (Yields 2x Primary in Centrifuge)"))
				.build());

		// Row 2 (y=4): Primary Ore
		builder.putSlot(PRIMARY, Slot.builder(g.grid(6, 4))
				.setTooltip(Tooltip.create("Primary Bedrock Ore\nCan be directly centrifuged or chemically leached"))
				.build());

		// Row 3 (y=6): Chemical Leaching
		builder.putSlot(PRIMARY_SULFURIC, Slot.builder(g.grid(2, 6))
				.setTooltip(Tooltip.create("Sulfuric Leached Ore\nLiberates Acid Byproduct in Centrifuge"))
				.build());
		builder.putSlot(PRIMARY_SOLVENT, Slot.builder(g.grid(6, 6))
				.setTooltip(Tooltip.create("Solvent Leached Ore\nLiberates Solvent & Acid Byproducts in Centrifuge"))
				.build());
		builder.putSlot(PRIMARY_RAD, Slot.builder(g.grid(10, 6))
				.setTooltip(Tooltip.create("Radiosolvent Leached Ore\nLiberates All 3 Byproducts in Centrifuge"))
				.build());

		// Row 4 (y=8): Separation Tier
		builder.putSlot(PRIMARY_CLEANED, Slot.builder(g.grid(0, 8))
				.setTooltip(Tooltip.create("Cleaned Primary Ore\nCentrifuge to extract Primary Material Fragments"))
				.build());
		builder.putSlot(SULFURIC_BYPRODUCT, Slot.builder(g.grid(4, 8))
				.setTooltip(Tooltip.create("Acid Byproduct Chunk\nRoast/Arc/Wash to extract Acid Byproduct Fragments"))
				.build());
		builder.putSlot(SOLVENT_BYPRODUCT, Slot.builder(g.grid(8, 8))
				.setTooltip(Tooltip.create("Solvent Byproduct Chunk\nRoast/Arc/Wash to extract Solvent Byproduct Fragments"))
				.build());
		builder.putSlot(RAD_BYPRODUCT, Slot.builder(g.grid(12, 8))
				.setTooltip(Tooltip.create("Radioactive Byproduct Chunk\nRoast/Arc/Wash to extract Rad Byproduct Fragments"))
				.build());

		// Row 5 (y=10): Byproduct Washed Endpoints
		builder.putSlot(SULFURIC_WASHED, Slot.builder(g.grid(4, 10))
				.setTooltip(Tooltip.create("Washed Acid Byproduct\nCentrifuge for Acid Byproduct Fragments"))
				.build());
		builder.putSlot(SOLVENT_WASHED, Slot.builder(g.grid(8, 10))
				.setTooltip(Tooltip.create("Washed Solvent Byproduct\nCentrifuge for Solvent Byproduct Fragments"))
				.build());
		builder.putSlot(RAD_WASHED, Slot.builder(g.grid(12, 10))
				.setTooltip(Tooltip.create("Washed Rad Byproduct\nCentrifuge for Rad Byproduct Fragments"))
				.build());

		// Row 6 (y=12): The 4 Final Fragments
		builder.putSlot(FRAGMENT_PRIMARY, Slot.builder(g.grid(0, 12))
				.setTooltip(Tooltip.create("Primary Material Fragment\nCraft 9 into ingot/gem/dust"))
				.build());
		builder.putSlot(FRAGMENT_ACID, Slot.builder(g.grid(4, 12))
				.setTooltip(Tooltip.create("Acid Byproduct Fragment\nCraft 9 into ingot/gem/dust"))
				.build());
		builder.putSlot(FRAGMENT_SOLVENT, Slot.builder(g.grid(8, 12))
				.setTooltip(Tooltip.create("Solvent Byproduct Fragment\nCraft 9 into ingot/gem/dust"))
				.build());
		builder.putSlot(FRAGMENT_RAD, Slot.builder(g.grid(12, 12))
				.setTooltip(Tooltip.create("Rad Byproduct Fragment\nCraft 9 into ingot/gem/dust"))
				.build());

		// Row 7 (y=14): Crumbs
		builder.putSlot(CRUMBS, Slot.builder(g.grid(6, 14))
				.setTooltip(Tooltip.create("Bedrock Crumbs\nRecycle in Electrolyser / Centrifuge"))
				.build());

		// Machine & Fluid Labels
		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_ore_slopper)), g.grid(5, 1)))
				.setTooltip(Tooltip.create("Ore Slopper\nClick to view machine recipes"))
				.setInteract("ntmOreSlopper")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(ItemFluidIcon.make(Fluids.WATER, 1000)), g.grid(7, 1)))
				.setTooltip(Tooltip.create("Water: 1,000 mB"))
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_pyrooven)), g.grid(4, 2)))
				.setTooltip(Tooltip.create("Combination / Pyro Oven\nClick to view recipes"))
				.setInteract("ntmPyrolysis")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_crystallizer)), g.grid(8, 2)))
				.setTooltip(Tooltip.create("Crystallizer (Washing)\nClick to view recipes"))
				.setInteract("ntmCrystallizer")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_centrifuge)), g.grid(6, 3)))
				.setTooltip(Tooltip.create("Centrifuge\nDirect path yields 1x Primary; Washed yields 2x Primary"))
				.setInteract("ntmCentrifuge")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(ItemFluidIcon.make(Fluids.SULFURIC_ACID, 250)), g.grid(2, 5)))
				.setTooltip(Tooltip.create("Sulfuric Acid: 250 mB"))
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(ItemFluidIcon.make(Fluids.SOLVENT, 250)), g.grid(6, 5)))
				.setTooltip(Tooltip.create("Solvent: 250 mB"))
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(ItemFluidIcon.make(Fluids.RADIOSOLVENT, 250)), g.grid(10, 5)))
				.setTooltip(Tooltip.create("Radiosolvent: 250 mB"))
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_centrifuge)), g.grid(6, 7)))
				.setTooltip(Tooltip.create("Centrifuge (Separation)\nSeparates cleaned primary & byproduct chunks"))
				.setInteract("ntmCentrifuge")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_crystallizer)), g.grid(4, 9)))
				.setTooltip(Tooltip.create("Crystallizer (Acid Byproduct Wash)\nRoast & Arc Furnace can increase yield"))
				.setInteract("ntmCrystallizer")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_crystallizer)), g.grid(8, 9)))
				.setTooltip(Tooltip.create("Crystallizer (Solvent Byproduct Wash)\nRoast & Arc Furnace can increase yield"))
				.setInteract("ntmCrystallizer")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_crystallizer)), g.grid(12, 9)))
				.setTooltip(Tooltip.create("Crystallizer (Rad Byproduct Wash)\nRoast & Arc Furnace can increase yield"))
				.setInteract("ntmCrystallizer")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_centrifuge)), g.grid(0, 11)))
				.setTooltip(Tooltip.create("Centrifuge\nExtracts Primary Fragments"))
				.setInteract("ntmCentrifuge")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_centrifuge)), g.grid(4, 11)))
				.setTooltip(Tooltip.create("Centrifuge\nExtracts Acid Byproduct Fragments"))
				.setInteract("ntmCentrifuge")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_centrifuge)), g.grid(8, 11)))
				.setTooltip(Tooltip.create("Centrifuge\nExtracts Solvent Byproduct Fragments"))
				.setInteract("ntmCentrifuge")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_centrifuge)), g.grid(12, 11)))
				.setTooltip(Tooltip.create("Centrifuge\nExtracts Rad Byproduct Fragments"))
				.setInteract("ntmCentrifuge")
				.build());

		builder.addInteractable(CustomInteractable.builder(
				ComponentLabel.create(ItemComponent.create(new ItemStack(ModBlocks.machine_electrolyser)), g.grid(8, 14)))
				.setTooltip(Tooltip.create("Electrolyser (Recycling)\nClick to view recycling recipes"))
				.setInteract("ntmElectrolysisMetal")
				.build());

		// Connecting Lines & Arrows (Strictly orthogonal segments)

		// 1. Raw Ore -> BASE
		builder.addLines(Lines.builder(g.bigEdge(6, 0, Direction.S))
				.addArrow(g.edge(6, 2, Direction.N))
				.build());

		// 2. BASE -> BASE_ROASTED (West)
		builder.addLines(Lines.builder(g.edge(6, 2, Direction.W))
				.addArrow(g.edge(2, 2, Direction.E))
				.build());

		// 3. BASE -> BASE_WASHED (East)
		builder.addLines(Lines.builder(g.edge(6, 2, Direction.E))
				.addArrow(g.edge(10, 2, Direction.W))
				.build());

		// 4. BASE / ROASTED / WASHED down to PRIMARY (6, 4)
		// Central stem from BASE through centrifuge to PRIMARY
		builder.addLines(Lines.builder(g.edge(6, 2, Direction.S))
				.addArrow(g.edge(6, 4, Direction.N))
				.build());

		// BASE_ROASTED down & east joining central stem at (6, 3)
		Point rDrop = Point.create(g.grid(2, 0).x(), g.grid(0, 3).y());
		Point rJoin = Point.create(g.grid(6, 0).x(), g.grid(0, 3).y());
		builder.addLines(Lines.builder(g.edge(2, 2, Direction.S))
				.addSegment(rDrop)
				.addSegment(rJoin)
				.build());

		// BASE_WASHED down & west joining central stem at (6, 3)
		Point wDrop = Point.create(g.grid(10, 0).x(), g.grid(0, 3).y());
		builder.addLines(Lines.builder(g.edge(10, 2, Direction.S))
				.addSegment(wDrop)
				.addSegment(rJoin)
				.build());

		// 5. PRIMARY down and fanning out to Chemical Branches (y=6)
		Point pStem = Point.create(g.grid(6, 0).x(), g.grid(0, 5).y());
		Point pBusL = Point.create(g.grid(2, 0).x(), g.grid(0, 5).y());
		Point pBusR = Point.create(g.grid(10, 0).x(), g.grid(0, 5).y());

		builder.addLines(Lines.builder(g.edge(6, 4, Direction.S)).addSegment(pStem).build());
		builder.addLines(Lines.builder(pBusL).addSegment(pBusR).build());
		builder.addLines(Lines.builder(pBusL).addArrow(g.edge(2, 6, Direction.N)).build());
		builder.addLines(Lines.builder(pStem).addArrow(g.edge(6, 6, Direction.N)).build());
		builder.addLines(Lines.builder(pBusR).addArrow(g.edge(10, 6, Direction.N)).build());

		// 6. Chemical Branches down to Separation Row 4 (y=8)
		Point chemDropL = Point.create(g.grid(2, 0).x(), g.grid(0, 7).y());
		Point chemDropM = Point.create(g.grid(6, 0).x(), g.grid(0, 7).y());
		Point chemDropR = Point.create(g.grid(10, 0).x(), g.grid(0, 7).y());

		builder.addLines(Lines.builder(g.edge(2, 6, Direction.S)).addSegment(chemDropL).build());
		builder.addLines(Lines.builder(g.edge(6, 6, Direction.S)).addSegment(chemDropM).build());
		builder.addLines(Lines.builder(g.edge(10, 6, Direction.S)).addSegment(chemDropR).build());

		// Horizontal separation bus at y=7
		Point sepBusL = Point.create(g.grid(0, 0).x(), g.grid(0, 7).y());
		Point sepBusR = Point.create(g.grid(12, 0).x(), g.grid(0, 7).y());
		builder.addLines(Lines.builder(sepBusL).addSegment(sepBusR).build());

		// Drops with arrows to Row 4
		Point sep4 = Point.create(g.grid(4, 0).x(), g.grid(0, 7).y());
		Point sep8 = Point.create(g.grid(8, 0).x(), g.grid(0, 7).y());
		builder.addLines(Lines.builder(sepBusL).addArrow(g.edge(0, 8, Direction.N)).build());
		builder.addLines(Lines.builder(sep4).addArrow(g.edge(4, 8, Direction.N)).build());
		builder.addLines(Lines.builder(sep8).addArrow(g.edge(8, 8, Direction.N)).build());
		builder.addLines(Lines.builder(sepBusR).addArrow(g.edge(12, 8, Direction.N)).build());

		// 7. Row 4 to Row 5 (Byproducts to Washed Byproducts)
		builder.addLines(Lines.builder(g.edge(4, 8, Direction.S)).addArrow(g.edge(4, 10, Direction.N)).build());
		builder.addLines(Lines.builder(g.edge(8, 8, Direction.S)).addArrow(g.edge(8, 10, Direction.N)).build());
		builder.addLines(Lines.builder(g.edge(12, 8, Direction.S)).addArrow(g.edge(12, 10, Direction.N)).build());

		// 8. Row 4 / Row 5 down to Final Fragments Row 6 (y=12)
		builder.addLines(Lines.builder(g.edge(0, 8, Direction.S)).addArrow(g.edge(0, 12, Direction.N)).build());
		builder.addLines(Lines.builder(g.edge(4, 10, Direction.S)).addArrow(g.edge(4, 12, Direction.N)).build());
		builder.addLines(Lines.builder(g.edge(8, 10, Direction.S)).addArrow(g.edge(8, 12, Direction.N)).build());
		builder.addLines(Lines.builder(g.edge(12, 10, Direction.S)).addArrow(g.edge(12, 12, Direction.N)).build());

		// 9. Fragments down to Crumbs Row 7 (y=14)
		Point cr0 = Point.create(g.grid(0, 0).x(), g.grid(0, 13).y());
		Point cr4 = Point.create(g.grid(4, 0).x(), g.grid(0, 13).y());
		Point cr8 = Point.create(g.grid(8, 0).x(), g.grid(0, 13).y());
		Point cr12 = Point.create(g.grid(12, 0).x(), g.grid(0, 13).y());
		Point crMid = Point.create(g.grid(6, 0).x(), g.grid(0, 13).y());

		builder.addLines(Lines.builder(g.edge(0, 12, Direction.S)).addSegment(cr0).build());
		builder.addLines(Lines.builder(g.edge(4, 12, Direction.S)).addSegment(cr4).build());
		builder.addLines(Lines.builder(g.edge(8, 12, Direction.S)).addSegment(cr8).build());
		builder.addLines(Lines.builder(g.edge(12, 12, Direction.S)).addSegment(cr12).build());
		builder.addLines(Lines.builder(cr0).addSegment(cr12).build());
		builder.addLines(Lines.builder(crMid).addArrow(g.edge(6, 14, Direction.N)).build());

		// Arrow from CRUMBS to Electrolyser
		builder.addLines(Lines.builder(g.edge(6, 14, Direction.E)).addArrow(g.edge(8, 14, Direction.W)).build());

		return builder.build();
	}

	private static Diagram buildDiagramForType(Layout baseLayout, CelestialBedrockOreType type) {
		Diagram.Builder d = Diagram.builder().addLayout(baseLayout);

		// Raw Ore Piece
		d.insertIntoSlot(RAW_ORE, DisplayComponent.builder(new ItemStack(ModItems.bedrock_ore_base, 1, type.body.ordinal())).build());

		// Base Tier
		d.insertIntoSlot(BASE_ROASTED, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.BASE_ROASTED, type)).build());
		d.insertIntoSlot(BASE, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.BASE, type)).build());
		d.insertIntoSlot(BASE_WASHED, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.BASE_WASHED, type)).build());

		// Primary Tier (2x yield from washed)
		d.insertIntoSlot(PRIMARY, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY, type, 2)).build());

		// Chemical Tier
		d.insertIntoSlot(PRIMARY_SULFURIC, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_SULFURIC, type)).build());
		d.insertIntoSlot(PRIMARY_SOLVENT, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_SOLVENT, type)).build());
		d.insertIntoSlot(PRIMARY_RAD, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_RAD, type)).build());

		// Separation Tier (Cycling between cleaned primary variants and byproduct chunks)
		d.insertIntoSlot(PRIMARY_CLEANED,
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_NORAD, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_NOSOLVENT, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.PRIMARY_NOSULFURIC, type)).build());

		d.insertIntoSlot(SULFURIC_BYPRODUCT, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SULFURIC_BYPRODUCT, type)).build());
		d.insertIntoSlot(SOLVENT_BYPRODUCT, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SOLVENT_BYPRODUCT, type)).build());
		d.insertIntoSlot(RAD_BYPRODUCT, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.RAD_BYPRODUCT, type)).build());

		// Byproduct Washed (Cycling with roasted and arc variants)
		d.insertIntoSlot(SULFURIC_WASHED,
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SULFURIC_WASHED, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SULFURIC_ARC, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SULFURIC_ROASTED, type)).build());

		d.insertIntoSlot(SOLVENT_WASHED,
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SOLVENT_WASHED, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SOLVENT_ARC, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.SOLVENT_ROASTED, type)).build());

		d.insertIntoSlot(RAD_WASHED,
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.RAD_WASHED, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.RAD_ARC, type)).build(),
				DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.RAD_ROASTED, type)).build());

		// Final Output Fragments
		d.insertIntoSlot(FRAGMENT_PRIMARY, DisplayComponent.builder(ItemBedrockOreNew.extract(type.primary, 1)).build());
		d.insertIntoSlot(FRAGMENT_ACID, DisplayComponent.builder(ItemBedrockOreNew.extract(type.byproductAcid, 1)).build());
		d.insertIntoSlot(FRAGMENT_SOLVENT, DisplayComponent.builder(ItemBedrockOreNew.extract(type.byproductSolvent, 1)).build());
		d.insertIntoSlot(FRAGMENT_RAD, DisplayComponent.builder(ItemBedrockOreNew.extract(type.byproductRad, 1)).build());

		// Crumbs
		d.insertIntoSlot(CRUMBS, DisplayComponent.builder(ItemBedrockOreNew.make(BedrockOreGrade.CRUMBS, type)).build());

		return d.build();
	}

	public static class HandlerInfoListener {
		private final DiagramGroupInfo info;

		public HandlerInfoListener(DiagramGroupInfo info) {
			this.info = info;
		}

		@SubscribeEvent
		public void onRegisterHandlerInfos(NEIRegisterHandlerInfosEvent event) {
			event.registerHandlerInfo(
					info.groupId(),
					RefStrings.NAME,
					RefStrings.MODID,
					info::buildHandlerInfo);
		}
	}
}
