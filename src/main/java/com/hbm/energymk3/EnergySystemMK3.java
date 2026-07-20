package com.hbm.energymk3;

import com.hbm.blocks.machine.*;
import com.hbm.blocks.network.*;
import com.hbm.items.tool.ItemWireSpool;
import com.hbm.items.tool.ItemWiringMK3;
import com.hbm.tileentity.machine.*;
import com.hbm.tileentity.machine.storage.TileEntityMachineBatteryMK3;
import com.hbm.tileentity.network.*;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class EnergySystemMK3 {

	// Cables
	public static Block cable_copper;
	public static Block cable_gold;
	public static Block cable_steel;
	public static Block cable_advanced;
	public static Block cable_superconductor;

	// Machines
	public static Block machine_coal_mk3;
	public static Block machine_electric_furnace_mk3;
	public static Block machine_battery_mk3;
	public static Block machine_gas_generator_mk3;
	public static Block machine_assembler_mk3;
	public static Block machine_centrifuge_mk3;
	public static Block machine_combustion_engine_mk3;
	public static Block machine_steam_turbine_mk3;

	// Network
	public static Block transformer_mk3;
	public static Block bridge_transformer_mk2_mk3;
	public static Block cable_fuse_mk3;
	public static Block bus_transformer_mk3;
	public static Block bus_bar_mk3;
	public static Block bus_bar_corner_mk3;
	public static Block bus_bar_t_junction_mk3;

	// Pylons
	public static Block pylon_connector_mk3;
	public static Block pylon_connector_super_mk3;
	public static Block pylon_small_mk3;
	public static Block pylon_medium_mk3;
	public static Block pylon_medium_transformer_mk3;
	public static Block pylon_large_mk3;
	public static Block pylon_large_transformer_mk3;
	public static Block substation_mk3;

	// Items
	public static Item wiring_mk3;
	public static Item wire_spool_copper;
	public static Item wire_spool_gold;
	public static Item wire_spool_steel;
	public static Item wire_spool_superconductor;

	public static void init() {
		initBlocks();
		initItems();
		registerBlocks();
		registerItems();
		registerTileEntities();
	}

	private static void initBlocks() {
		cable_copper = new CableCopper().setBlockName("cable_copper_mk3").setHardness(1.0F);
		cable_gold = new CableGold().setBlockName("cable_gold_mk3").setHardness(1.0F);
		cable_steel = new CableSteel().setBlockName("cable_steel_mk3").setHardness(2.0F);
		cable_advanced = new CableAdvanced().setBlockName("cable_advanced_mk3").setHardness(3.0F);
		cable_superconductor = new CableSuperconductor().setBlockName("cable_superconductor_mk3").setHardness(4.0F);

		machine_coal_mk3 = new MachineCoalMK3().setBlockName("machine_coal_mk3").setHardness(3.0F);
		machine_electric_furnace_mk3 = new MachineElectricFurnaceMK3().setBlockName("machine_electric_furnace_mk3").setHardness(3.0F);
		machine_battery_mk3 = new MachineBatteryMK3().setBlockName("machine_battery_mk3").setHardness(3.0F);
		machine_gas_generator_mk3 = new MachineGasGeneratorMK3().setBlockName("machine_gas_generator_mk3").setHardness(3.0F);
		machine_assembler_mk3 = new MachineAssemblerMK3().setBlockName("machine_assembler_mk3").setHardness(3.0F);
		machine_centrifuge_mk3 = new MachineCentrifugeMK3().setBlockName("machine_centrifuge_mk3").setHardness(3.0F);
		machine_combustion_engine_mk3 = new MachineCombustionEngineMK3().setBlockName("machine_combustion_engine_mk3").setHardness(3.0F);
		machine_steam_turbine_mk3 = new MachineSteamTurbineMK3().setBlockName("machine_steam_turbine_mk3").setHardness(3.0F);

		transformer_mk3 = new BlockTransformerMK3().setBlockName("transformer_mk3").setHardness(3.0F);
		bridge_transformer_mk2_mk3 = new BlockBridgeTransformerMK2MK3().setBlockName("bridge_transformer_mk2_mk3").setHardness(3.0F);
		cable_fuse_mk3 = new BlockCableFuseMK3().setBlockName("cable_fuse_mk3").setHardness(2.0F);
		bus_transformer_mk3 = new BlockBusTransformerMK3().setBlockName("bus_transformer_mk3").setHardness(3.0F);
		bus_bar_mk3 = new BlockBusBarMK3().setBlockName("bus_bar_mk3").setHardness(5.0F);
		bus_bar_corner_mk3 = new BlockBusBarCornerMK3().setBlockName("bus_bar_corner_mk3").setHardness(5.0F);
		bus_bar_t_junction_mk3 = new BlockBusBarTJunctionMK3().setBlockName("bus_bar_t_junction_mk3").setHardness(5.0F);

		pylon_connector_mk3 = new PylonConnectorMK3().setBlockName("pylon_connector_mk3").setHardness(3.0F);
		pylon_connector_super_mk3 = new PylonConnectorSuperMK3().setBlockName("pylon_connector_super_mk3").setHardness(3.0F);
		pylon_small_mk3 = new PylonSmallMK3().setBlockName("pylon_small_mk3").setHardness(3.0F);
		pylon_medium_mk3 = new PylonMediumMK3(false).setBlockName("pylon_medium_mk3").setHardness(3.0F);
		pylon_medium_transformer_mk3 = new PylonMediumMK3(true).setBlockName("pylon_medium_transformer_mk3").setHardness(3.0F);
		pylon_large_mk3 = new PylonLargeMK3(false).setBlockName("pylon_large_mk3").setHardness(3.0F);
		pylon_large_transformer_mk3 = new PylonLargeMK3(true).setBlockName("pylon_large_transformer_mk3").setHardness(3.0F);
		substation_mk3 = new SubstationMK3().setBlockName("substation_mk3").setHardness(5.0F);
	}

	private static void initItems() {
		wiring_mk3 = new ItemWiringMK3().setUnlocalizedName("wiring_mk3").setTextureName("hbm:wiring_mk3").setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabTools);
		wire_spool_copper = new ItemWireSpool(0).setUnlocalizedName("wire_spool_copper_mk3").setTextureName("hbm:wire_spool_copper");
		wire_spool_gold = new ItemWireSpool(1).setUnlocalizedName("wire_spool_gold_mk3").setTextureName("hbm:wire_spool_gold");
		wire_spool_steel = new ItemWireSpool(2).setUnlocalizedName("wire_spool_steel_mk3").setTextureName("hbm:wire_spool_steel");
		wire_spool_superconductor = new ItemWireSpool(3).setUnlocalizedName("wire_spool_superconductor_mk3").setTextureName("hbm:wire_spool_superconductor");
	}

	private static void registerBlocks() {
		register(cable_copper);
		register(cable_gold);
		register(cable_steel);
		register(cable_advanced);
		register(cable_superconductor);
		register(machine_coal_mk3);
		register(machine_electric_furnace_mk3);
		register(machine_battery_mk3);
		register(machine_gas_generator_mk3);
		register(machine_assembler_mk3);
		register(machine_centrifuge_mk3);
		register(machine_combustion_engine_mk3);
		register(machine_steam_turbine_mk3);
		register(transformer_mk3);
		register(bridge_transformer_mk2_mk3);
		register(cable_fuse_mk3);
		register(bus_transformer_mk3);
		register(bus_bar_mk3);
		register(bus_bar_corner_mk3);
		register(bus_bar_t_junction_mk3);
		register(pylon_connector_mk3);
		register(pylon_connector_super_mk3);
		register(pylon_small_mk3);
		register(pylon_medium_mk3);
		register(pylon_medium_transformer_mk3);
		register(pylon_large_mk3);
		register(pylon_large_transformer_mk3);
		register(substation_mk3);
	}

	private static void registerItems() {
		GameRegistry.registerItem(wiring_mk3, wiring_mk3.getUnlocalizedName());
		GameRegistry.registerItem(wire_spool_copper, wire_spool_copper.getUnlocalizedName());
		GameRegistry.registerItem(wire_spool_gold, wire_spool_gold.getUnlocalizedName());
		GameRegistry.registerItem(wire_spool_steel, wire_spool_steel.getUnlocalizedName());
		GameRegistry.registerItem(wire_spool_superconductor, wire_spool_superconductor.getUnlocalizedName());
	}

	private static void register(Block b) {
		GameRegistry.registerBlock(b, b.getUnlocalizedName());
	}

	private static void registerTileEntities() {
		GameRegistry.registerTileEntity(TileEntityCableMK3.class, "tileentity_cable_mk3");

		GameRegistry.registerTileEntity(TileEntityMachineCoalMK3.class, "tileentity_coal_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineElectricFurnaceMK3.class, "tileentity_electric_furnace_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineBatteryMK3.class, "tileentity_battery_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineGasGeneratorMK3.class, "tileentity_gas_generator_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineAssemblerMK3.class, "tileentity_assembler_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineCentrifugeMK3.class, "tileentity_centrifuge_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineCombustionEngineMK3.class, "tileentity_combustion_engine_mk3");
		GameRegistry.registerTileEntity(TileEntityMachineSteamTurbineMK3.class, "tileentity_steam_turbine_mk3");

		GameRegistry.registerTileEntity(TileEntityTransformerMK3.class, "tileentity_transformer_mk3");
		GameRegistry.registerTileEntity(TileEntityBridgeTransformerMK2MK3.class, "tileentity_bridge_transformer_mk2_mk3");
		GameRegistry.registerTileEntity(TileEntityCableFuseMK3.class, "tileentity_cable_fuse_mk3");
		GameRegistry.registerTileEntity(TileEntityBusTransformerMK3.class, "tileentity_bus_transformer_mk3");

		GameRegistry.registerTileEntity(TileEntityBusBarMK3.class, "tileentity_bus_bar_mk3");
		GameRegistry.registerTileEntity(TileEntityBusBarCornerMK3.class, "tileentity_bus_bar_corner_mk3");
		GameRegistry.registerTileEntity(TileEntityBusBarTJunctionMK3.class, "tileentity_bus_bar_t_junction_mk3");

		GameRegistry.registerTileEntity(TileEntityPylonConnectorMK3.class, "tileentity_pylon_connector_mk3");
		GameRegistry.registerTileEntity(TileEntityPylonConnectorSuperMK3.class, "tileentity_pylon_connector_super_mk3");
		GameRegistry.registerTileEntity(TileEntityPylonSmallMK3.class, "tileentity_pylon_small_mk3");
		GameRegistry.registerTileEntity(TileEntityPylonMediumMK3.class, "tileentity_pylon_medium_mk3");
		GameRegistry.registerTileEntity(TileEntityPylonLargeMK3.class, "tileentity_pylon_large_mk3");
		GameRegistry.registerTileEntity(TileEntitySubstationMK3.class, "tileentity_substation_mk3");
	}
}
