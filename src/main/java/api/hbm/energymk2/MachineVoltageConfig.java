package api.hbm.energymk2;

import java.util.HashSet;
import java.util.Set;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

public final class MachineVoltageConfig {

	private static final long LV = VoltageTier.LV;
	private static final long MV = VoltageTier.MV;
	private static final long HV = VoltageTier.HV;
	private static final long EV = VoltageTier.EV;
	private static final long IV = VoltageTier.IV;
	private static final long LUV = VoltageTier.LUV;
	private static final long ZPM = VoltageTier.ZPM;
	private static final long UV = VoltageTier.UV;
	private static final long UHV = VoltageTier.UHV;
	private static final long UEV = VoltageTier.UEV;
	private static final long UIV = VoltageTier.UIV;
	private static final long UMV = VoltageTier.UMV;
	private static final long UXV = VoltageTier.UXV;
	private static final long UNKNOWN = VoltageTier.UNKNOWN;
	private static final Set<Block> HIDDEN_VOLTAGE_TOOLTIPS = new HashSet<Block>();

	private MachineVoltageConfig() { }

	static boolean registerAll() {
		if(ModBlocks.machine_battery_socket == null) return false;

		configureOverrides();
		configureBatteryOverrides();
		return true;
	}

	private static void configureOverrides() {
		set(ModBlocks.machine_electric_furnace_off, LV);
		set(ModBlocks.machine_electric_furnace_on, LV);
		set(ModBlocks.machine_battery_redd, LV);
		set(ModBlocks.machine_centrifuge, LV);
		set(ModBlocks.machine_shredder, LV);
		set(ModBlocks.machine_diesel, LV);
		set(ModBlocks.machine_solar, LV);
		set(ModBlocks.machine_stirling, LV);
		set(ModBlocks.machine_stirling_steel, LV);
		set(ModBlocks.machine_stirling_creative, LV);
		set(ModBlocks.machine_steam_engine, LV);
		set(ModBlocks.machine_turbine, LV);
		set(ModBlocks.machine_turbofan, LV);
		set(ModBlocks.machine_turbinegas, LV);
		set(ModBlocks.machine_converter_he_rf, LV);
		set(ModBlocks.machine_converter_rf_he, LV);
		set(ModBlocks.machine_assembly_machine, LV);
		set(ModBlocks.machine_assembly_factory, LV);
		set(ModBlocks.machine_arc_furnace, LV);
		set(ModBlocks.machine_chemical_plant, LV);
		set(ModBlocks.machine_chemical_factory, LV);
	}

	private static void configureBatteryOverrides() {
		setBattery(ModItems.battery_pack, EnumBatteryPack.BATTERY_REDSTONE.ordinal(), LV);
		setBattery(ModItems.battery_pack, EnumBatteryPack.BATTERY_LEAD.ordinal(), LV);
		setBattery(ModItems.battery_pack, EnumBatteryPack.BATTERY_LITHIUM.ordinal(), MV);
		setBattery(ModItems.battery_pack, EnumBatteryPack.BATTERY_SODIUM.ordinal(), MV);
		setBattery(ModItems.battery_pack, EnumBatteryPack.BATTERY_SCHRABIDIUM.ordinal(), HV);
		setBattery(ModItems.battery_pack, EnumBatteryPack.BATTERY_QUANTUM.ordinal(), EV);

		setBattery(ModItems.elec_sword, LV);
		setBattery(ModItems.elec_pickaxe, LV);
		setBattery(ModItems.elec_axe, LV);
		setBattery(ModItems.elec_shovel, LV);
	}

	private static void set(Block block, long voltage) {
		set(block, voltage, VoltageTier.DEFAULT_EXPLOSION_STRENGTH, true);
	}

	private static void set(Block block, long voltage, boolean showVoltageTooltip) {
		set(block, voltage, VoltageTier.DEFAULT_EXPLOSION_STRENGTH, showVoltageTooltip);
	}

	private static void set(Block block, long voltage, float explosionStrength) {
		set(block, voltage, explosionStrength, true);
	}

	private static void set(Block block, long voltage, float explosionStrength, boolean showVoltageTooltip) {
		if(block == null) return;
		MachineVoltageRegistry.setVoltage(block, voltage, explosionStrength);
		if(showVoltageTooltip) HIDDEN_VOLTAGE_TOOLTIPS.remove(block);
		else HIDDEN_VOLTAGE_TOOLTIPS.add(block);
	}

	private static void setBattery(Item item, long voltage) {
		BatteryVoltageRegistry.setVoltage(item, voltage);
	}

	private static void setBattery(Item item, int damage, long voltage) {
		BatteryVoltageRegistry.setVoltage(item, damage, voltage);
	}

	public static boolean shouldShowVoltageTooltip(Block block) {
		if(block == null) return false;
		long voltage = MachineVoltageRegistry.getVoltageForBlock(block, VoltageTier.DEFAULT);
		if(!VoltageTier.isConfigured(voltage)) return false;
		return !HIDDEN_VOLTAGE_TOOLTIPS.contains(block);
	}
}
