package com.hbm.inventory.recipes;

import static com.hbm.inventory.OreDictManager.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.FluidStack;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ItemGenericPart.EnumPartType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemArcElectrode.EnumElectrodeType;
import com.hbm.items.machine.ItemCircuit.EnumCircuitType;
import com.hbm.items.machine.ItemFluidIcon;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Loader;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public class ArcWelderRecipes extends SerializableRecipe {

	public static List<ArcWelderRecipe> recipes = new ArrayList();

	@Override
	public void registerDefaults() {

		//Parts
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.motor, 2), 100, 400L,
				new OreDictStack(STEEL.plate(), 2), new OreDictStack(MINGRADE.wireDense(), 2)));
		recipes.add(new ArcWelderRecipe(DictFrame.fromOne(ModItems.part_generic, EnumPartType.LDE), 200, 5_000L,
				new OreDictStack(AL.plate(), 4), new OreDictStack(FIBER.ingot(), 4), new OreDictStack(ANY_HARDPLASTIC.ingot())));
		recipes.add(new ArcWelderRecipe(DictFrame.fromOne(ModItems.part_generic, EnumPartType.LDE), 200, 10_000L,
				new OreDictStack(TI.plate(), 2), new OreDictStack(FIBER.ingot(), 4), new OreDictStack(ANY_HARDPLASTIC.ingot())));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.neutron_reflector, 2), 400, 50_000L,
				new OreDictStack(WC.ingot(), 2), new OreDictStack(DURA.plate())));

		//Dense Wires
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.wire_dense, 1, Mats.MAT_COPPER.id), 100, 10_000L,
				new OreDictStack(CU.wireFine(), 8)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.wire_dense, 1, Mats.MAT_MINGRADE.id), 100, 10_000L,
				new OreDictStack(MINGRADE.wireFine(), 8)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.wire_dense, 1, Mats.MAT_GOLD.id), 100, 10_000L,
				new OreDictStack(GOLD.wireFine(), 8)));

		//earlygame welded parts
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_IRON.id), 100, 100L,
				new OreDictStack(IRON.plateCast(), 2)));
		//high-demand mid-game parts
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_STEEL.id), 100, 500L,
				new OreDictStack(STEEL.plateCast(), 2)));
		//literally just the combination oven
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_COPPER.id), 200, 1_000L,
				new OreDictStack(CU.plateCast(), 2)));
		//mid-game, single combustion engine running on LPG
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_TITANIUM.id), 600, 50_000L,
				new OreDictStack(TI.plateCast(), 2)));
		//mid-game PWR
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_ZIRCONIUM.id), 600, 10_000L,
				new OreDictStack(ZR.plateCast(), 2)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_ALUMINIUM.id), 300, 10_000L,
				new OreDictStack(AL.plateCast(), 2)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_STAINLESS.id), 250, 20_000L,
				new OreDictStack(STAINLESS.plateCast(), 2)));
		//late-game fusion
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_TCALLOY.id), 1_200, 1_000_000L, new FluidStack(Fluids.OXYGEN, 1_000),
				new OreDictStack(TCALLOY.plateCast(), 2)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_CDALLOY.id), 1_200, 1_000_000L, new FluidStack(Fluids.OXYGEN, 1_000),
				new OreDictStack(CDALLOY.plateCast(), 2)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_TUNGSTEN.id), 1_200, 250_000L, new FluidStack(Fluids.OXYGEN, 1_000),
				new OreDictStack(W.plateCast(), 2)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_CMB.id), 1_200, 10_000_000L, new FluidStack(Fluids.REFORMGAS, 1_000),
				new OreDictStack(CMB.plateCast(), 2)));
		//pre-DFC
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.plate_welded, 1, Mats.MAT_OSMIRIDIUM.id), 6_000, 50_000_000L, new FluidStack(Fluids.REFORMGAS, 16_000),
				new OreDictStack(OSMIRIDIUM.plateCast(), 2)));

		//Missile Parts
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.thruster_small), 60, 1_000L, new OreDictStack(STEEL.plate(), 4), new OreDictStack(AL.wireFine(), 4), new OreDictStack(CU.plate(), 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.thruster_medium), 100, 2_000L, new OreDictStack(STEEL.plate(), 8), new ComparableStack(ModItems.motor, 1), new OreDictStack(GRAPHITE.ingot(), 8)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.thruster_large), 200, 5_000L, new OreDictStack(DURA.ingot(), 10), new ComparableStack(ModItems.motor, 1), new OreDictStack(WC.ingot(), 12)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.fuel_tank_small), 60, 1_000L, new OreDictStack(AL.plate(), 6), new OreDictStack(CU.plate(), 4), new ComparableStack(ModBlocks.steel_scaffold, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.fuel_tank_medium), 100, 2_000L, new OreDictStack(AL.plateCast(), 4), new OreDictStack(TI.plate(), 8), new ComparableStack(ModBlocks.steel_scaffold, 12)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.fuel_tank_large), 200, 5_000L, new OreDictStack(AL.plateWelded(), 8), new OreDictStack(BIGMT.plate(), 12), new ComparableStack(ModBlocks.steel_scaffold, 16)));

		//Missiles
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_anti_ballistic), 100, 5_000L, new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 3), new ComparableStack(ModItems.missile_assembly), new ComparableStack(ModItems.thruster_small, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_generic), 100, 5_000L, new ComparableStack(ModItems.warhead_generic_small), new ComparableStack(ModItems.fuel_tank_small), new ComparableStack(ModItems.thruster_small)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_incendiary), 100, 5_000L, new ComparableStack(ModItems.warhead_incendiary_small), new ComparableStack(ModItems.fuel_tank_small), new ComparableStack(ModItems.thruster_small)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_cluster), 100, 5_000L, new ComparableStack(ModItems.warhead_cluster_small), new ComparableStack(ModItems.fuel_tank_small), new ComparableStack(ModItems.thruster_small)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_buster), 100, 5_000L, new ComparableStack(ModItems.warhead_buster_small), new ComparableStack(ModItems.fuel_tank_small), new ComparableStack(ModItems.thruster_small)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_decoy), 60, 2_500L, new OreDictStack(STEEL.ingot()), new ComparableStack(ModItems.fuel_tank_small), new ComparableStack(ModItems.thruster_small)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_strong), 200, 10_000L, new ComparableStack(ModItems.warhead_generic_medium), new ComparableStack(ModItems.fuel_tank_medium), new ComparableStack(ModItems.thruster_medium)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_incendiary_strong), 200, 10_000L, new ComparableStack(ModItems.warhead_incendiary_medium), new ComparableStack(ModItems.fuel_tank_medium), new ComparableStack(ModItems.thruster_medium)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_cluster_strong), 200, 10_000L, new ComparableStack(ModItems.warhead_cluster_medium), new ComparableStack(ModItems.fuel_tank_medium), new ComparableStack(ModItems.thruster_medium)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_buster_strong), 200, 10_000L, new ComparableStack(ModItems.warhead_buster_medium), new ComparableStack(ModItems.fuel_tank_medium), new ComparableStack(ModItems.thruster_medium)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_emp_strong), 200, 10_000L, new ComparableStack(ModBlocks.emp_bomb, 3), new ComparableStack(ModItems.fuel_tank_medium), new ComparableStack(ModItems.thruster_medium)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_burst), 300, 25_000L, new ComparableStack(ModItems.warhead_generic_large), new ComparableStack(ModItems.fuel_tank_medium, 2), new ComparableStack(ModItems.thruster_medium, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_inferno), 300, 25_000L, new ComparableStack(ModItems.warhead_incendiary_large), new ComparableStack(ModItems.fuel_tank_medium, 2), new ComparableStack(ModItems.thruster_medium, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_rain), 300, 25_000L, new ComparableStack(ModItems.warhead_cluster_large), new ComparableStack(ModItems.fuel_tank_medium, 2), new ComparableStack(ModItems.thruster_medium, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_drill), 300, 25_000L, new ComparableStack(ModItems.warhead_buster_large), new ComparableStack(ModItems.fuel_tank_medium, 2), new ComparableStack(ModItems.thruster_medium, 4)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_nuclear), 600, 50_000L, new ComparableStack(ModItems.warhead_nuclear), new ComparableStack(ModItems.fuel_tank_large), new ComparableStack(ModItems.thruster_large, 3)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_nuclear_cluster), 600, 50_000L, new ComparableStack(ModItems.warhead_mirv), new ComparableStack(ModItems.fuel_tank_large), new ComparableStack(ModItems.thruster_large, 3)));
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.missile_volcano), 600, 50_000L, new ComparableStack(ModItems.warhead_volcano), new ComparableStack(ModItems.fuel_tank_large), new ComparableStack(ModItems.thruster_large, 3)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModBlocks.machine_xenon_thruster), 200, 50_000L, new FluidStack(Fluids.ARGON, 1_000), new OreDictStack(W.plateWelded(), 2), new OreDictStack(STAINLESS.plate(), 6), new ComparableStack(ModItems.arc_electrode, 1, EnumElectrodeType.GRAPHITE)));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.rp_fuselage_20_1), 100, 20_000L, new OreDictStack(STAINLESS.plateWelded(), 1), new ComparableStack(ModItems.seg_20, 2), new OreDictStack(TI.shell(), 1))); // 1 welded stainless, 1 titanium shell
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.rp_fuselage_20_3), 150, 30_000L, new OreDictStack(STAINLESS.plateWelded(), 1), new ComparableStack(ModItems.rp_fuselage_20_1), new OreDictStack(TI.shell(), 2))); // 2 weld stain, 3 tit shells
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.rp_fuselage_20_6), 200, 50_000L, new OreDictStack(STAINLESS.plateWelded(), 2), new ComparableStack(ModItems.rp_fuselage_20_3), new OreDictStack(TI.shell(), 3))); // 4 wain, 6 titties
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.rp_fuselage_20_12), 250, 60_000L, new FluidStack(Fluids.OXYGEN, 500), new OreDictStack(STAINLESS.plateWelded(), 4), new ComparableStack(ModItems.rp_fuselage_20_6), new OreDictStack(TI.shell(), 6))); // 8 win, 12 tit

		// space misc
		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.insert_cmb), 600, 50_000L, new FluidStack(Fluids.NEON, 2_000), new OreDictStack(CMB.plate(), 2), new OreDictStack(U238.ingot())));

		recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.AVIONICS.ordinal()), 250, 25_000L, new OreDictStack(AL.plateCast(), 2), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.AERO)));



		// NTNH RECIPES
		// case
		recipes.add(new ArcWelderRecipe(new ItemStack(ModBlocks.block_case), 200, 250L, new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(STEEL.ingot(), 2)));

		if(Loader.isModLoaded("ae2fc") && Loader.isModLoaded("appliedenergistics2")) {
		// Fluid storage cells
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage1")), 200, 250L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 0), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 0)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage4")), 200, 400L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 0), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 1)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage16")), 300, 600L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 0), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 2)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage64")), 350, 1_000L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 0), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 3)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage256")), 400, 1_500L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 1), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage1024")), 450, 1_800L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 1), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 5)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage4096")), 500, 2_000L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 1), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 6)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage16384")), 550, 2_500L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 1), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 7)));

		// Multi-fluid storage cells
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage1")), 200, 250L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 2), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 0)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage4")), 200, 400L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 2), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 1)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage16")), 300, 600L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 2), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 2)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage64")), 350, 1_000L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 2), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 3)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage256")), 400, 1_500L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 3), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 4)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage1024")), 450, 1_800L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 3), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 5)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage4096")), 500, 2_000L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 3), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 6)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage16384")), 550, 2_500L, new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage_housing"), 1, 3), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_part"), 1, 7)));

		// Creative storage cells
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage.singularity")), 600, 3_000L, new FluidStack(Fluids.STELLAR_FLUX, 1000), new ComparableStack(GameRegistry.findItem("ae2fc", "fluid_storage.quantum")), new ComparableStack(ModItems.singularity_spark)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("ae2fc", "fluid_storage.Universe")), 600, 4_000L, new FluidStack(Fluids.STELLAR_FLUX, 1000), new ComparableStack(GameRegistry.findItem("ae2fc", "multi_fluid_storage16384")), new ComparableStack(ModItems.singularity_spark)));
		}

		if(Loader.isModLoaded("appliedenergistics2")) {
		// Storage cells
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemBasicStorageCell.1k")), 200, 250L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 35), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemBasicStorageCell.4k")), 250, 400L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 36), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemBasicStorageCell.16k")), 300, 600L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 37), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemBasicStorageCell.64k")), 350, 1_000L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 38), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemAdvancedStorageCell.256k")), 400, 1_500L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 57), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 61)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemAdvancedStorageCell.1024k")), 450, 1_800, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 58), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 61)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemAdvancedStorageCell.4096k")), 500, 2_500, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 59), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 61)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemAdvancedStorageCell.16384k")), 550, 2_500L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 60), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 61)));

		// Spatial storage cells
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemSpatialStorageCell.2Cubed")), 250, 400L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 32), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemSpatialStorageCell.16Cubed")), 300, 600L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 33), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemSpatialStorageCell.128Cubed")), 350, 1_000L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 34), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 39)));

		// Creative storage cells
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Container")), 350, 2_000L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 38), new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 61)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Universe")), 600, 3_000L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemAdvancedStorageCell.16384k")), new ComparableStack(ModItems.singularity_spark)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Singularity")), 600, 4_000L, new ComparableStack(GameRegistry.findItem("appliedenergistics2", "item.ItemExtremeStorageCell.Quantum")), new ComparableStack(ModItems.singularity_spark)));
		}

		if(Loader.isModLoaded("matter-manipulator")) {
		// Matter Manipulator parts
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 8), 200, 250L, new ComparableStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 3), new OreDictStack(CO.billet(), 3), new OreDictStack(CO.nugget(), 5)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 7), 200, 250L, new ComparableStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 2), new OreDictStack(CO.billet(), 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.AVIONICS.ordinal())));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 6), 200, 250L, new ComparableStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 1), new OreDictStack(W.wireFine(), 8), new ComparableStack(ModItems.battery_sc, 1)));
		recipes.add(new ArcWelderRecipe(new ItemStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 9), 200, 250L, new ComparableStack(GameRegistry.findItem("matter-manipulator", "metaitem"), 1, 4), new OreDictStack(NB.wireDense(), 1)));
		}
	}


	public static HashMap getRecipes() {

		HashMap<Object, Object> recipes = new HashMap<Object, Object>();

		for(ArcWelderRecipe recipe : ArcWelderRecipes.recipes) {

			int size = recipe.ingredients.length + (recipe.fluid != null ? 1 : 0);
			Object[] array = new Object[size];

			for(int i = 0; i < recipe.ingredients.length; i++) {
				array[i] = recipe.ingredients[i];
			}

			if(recipe.fluid != null) array[size - 1] = ItemFluidIcon.make(recipe.fluid);

			recipes.put(array, recipe.output);
		}

		return recipes;
	}

	public static ArcWelderRecipe getRecipe(ItemStack... inputs) {

		outer:
		for(ArcWelderRecipe recipe : recipes) {

			List<AStack> recipeList = new ArrayList();
			for(AStack ingredient : recipe.ingredients) recipeList.add(ingredient);

			for(int i = 0; i < inputs.length; i++) {

				ItemStack inputStack = inputs[i];

				if(inputStack != null) {

					boolean hasMatch = false;
					Iterator<AStack> iterator = recipeList.iterator();

					while(iterator.hasNext()) {
						AStack recipeStack = iterator.next();

						if(recipeStack.matchesRecipe(inputStack, true) && inputStack.stackSize >= recipeStack.stacksize) {
							hasMatch = true;
							recipeList.remove(recipeStack);
							break;
						}
					}

					if(!hasMatch) {
						continue outer;
					}
				}
			}

			if(recipeList.isEmpty()) return recipe;
		}

		return null;
	}

	@Override
	public String getFileName() {
		return "hbmArcWelder.json";
	}

	@Override
	public Object getRecipeObject() {
		return recipes;
	}

	@Override
	public void deleteRecipes() {
		recipes.clear();
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = (JsonObject) recipe;

		AStack[] inputs = this.readAStackArray(obj.get("inputs").getAsJsonArray());
		FluidStack fluid = obj.has("fluid") ? this.readFluidStack(obj.get("fluid").getAsJsonArray()) : null;
		ItemStack output = this.readItemStack(obj.get("output").getAsJsonArray());
		int duration = obj.get("duration").getAsInt();
		long consumption = obj.get("consumption").getAsLong();

		recipes.add(new ArcWelderRecipe(output, duration, consumption, fluid, inputs));
	}

	@Override
	public void writeRecipe(Object obj, JsonWriter writer) throws IOException {
		ArcWelderRecipe recipe = (ArcWelderRecipe) obj;

		writer.name("inputs").beginArray();
		for(AStack aStack : recipe.ingredients) {
			this.writeAStack(aStack, writer);
		}
		writer.endArray();

		if(recipe.fluid != null) {
			writer.name("fluid");
			this.writeFluidStack(recipe.fluid, writer);
		}

		writer.name("output");
		this.writeItemStack(recipe.output, writer);

		writer.name("duration").value(recipe.duration);
		writer.name("consumption").value(recipe.consumption);
	}

	public static class ArcWelderRecipe {

		public AStack[] ingredients;
		public FluidStack fluid;
		public ItemStack output;
		public int duration;
		public long consumption;

		public ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {
			this.ingredients = ingredients;
			this.fluid = fluid;
			this.output = output;
			this.duration = duration;
			this.consumption = consumption;
		}

		public ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
			this(output, duration, consumption, null, ingredients);
		}
	}
}
