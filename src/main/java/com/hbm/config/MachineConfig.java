package com.hbm.config;

import net.minecraftforge.common.config.Configuration;

public class MachineConfig {
	
	protected static boolean scaleRTGPower = false;
	protected static boolean doRTGsDecay = true;

	// 0 = weighted sum, 1 = derived stats, 2 = hybrid
	public static int customFuelModel = 2;
	// 0 = recycle only, 1 = reprocess only, 2 = both
	public static int customFuelSpentPath = 2;
	// 0 = closest vanilla spent item, 1 = PUREX recipes, 2 = both
	public static int customFuelReprocessing = 2;

	public static void loadFromConfig(Configuration config) {

		final String CATEGORY_MACHINE = CommonConfig.CATEGORY_MACHINES;

		scaleRTGPower = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.00_scaleRTGPower", "Should RTG/Betavoltaic fuel power scale down as it decays?", false);
		doRTGsDecay = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.01_doRTGsDecay", "Should RTG/Betavoltaic fuel decay at all?", true);

		customFuelModel = CommonConfig.createConfigInt(config, CATEGORY_MACHINE, "9.02_customFuelModel",
			"How Moxer custom fuel turns its isotope mix into reactor behaviour. "
			+ "0 = weighted sum: every isotope reacts with its own curve, scaled by its share; "
			+ "1 = derived stats: the mix is turned into an equivalent classic fuel (enrichment tier, strength, heat); "
			+ "2 = hybrid: weighted curves with a neutron economy (fertile isotopes absorb and breed, schrabidium catalyses, diverse mixes run cooler)", 2, 0, 2);
		customFuelSpentPath = CommonConfig.createConfigInt(config, CATEGORY_MACHINE, "9.03_customFuelSpentPath",
			"What happens to spent Moxer custom fuel. "
			+ "0 = recycle: burn-up shifts the mix and the spent fuel goes back into the Moxer; "
			+ "1 = reprocess: spent fuel goes through the normal reprocessing chain; "
			+ "2 = both", 2, 0, 2);
		customFuelReprocessing = CommonConfig.createConfigInt(config, CATEGORY_MACHINE, "9.04_customFuelReprocessing",
			"How spent Moxer custom fuel is reprocessed (ignored when 9.03 is 0). "
			+ "0 = it becomes the closest vanilla spent item (by dominant isotope and enrichment); "
			+ "1 = PUREX recipes that recover isotopes matching its actual composition; "
			+ "2 = both (PUREX, or craft it into the closest vanilla spent item)", 2, 0, 2);
	}

	public static boolean customFuelRecycles() { return customFuelSpentPath != 1; }
	public static boolean customFuelReprocesses() { return customFuelSpentPath != 0; }
	public static boolean customFuelDirectVanilla() { return customFuelSpentPath == 1 && customFuelReprocessing == 0; }
	public static boolean customFuelPUREX() { return customFuelReprocesses() && customFuelReprocessing != 0; }
	public static boolean customFuelVanillaCraft() { return customFuelReprocesses() && customFuelReprocessing != 1 && !customFuelDirectVanilla(); }

}
