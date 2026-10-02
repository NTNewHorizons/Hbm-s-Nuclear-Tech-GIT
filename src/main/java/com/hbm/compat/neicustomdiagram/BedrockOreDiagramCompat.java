package com.hbm.compat.neicustomdiagram;

import cpw.mods.fml.common.Loader;

/**
 * Isolated entry point for NEI Custom Diagram integration.
 * Prevents classloader issues when nei-custom-diagram is not installed.
 */
public class BedrockOreDiagramCompat {

	private static boolean initialized = false;

	public static void init() {
		if(initialized) return;
		if(Loader.isModLoaded("neicustomdiagram")) {
			initialized = true;
			BedrockOreDiagramHandler.register();
		}
	}
}
