package com.hbm.render.tileentity;

import java.util.HashMap;

import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HFRWavefrontObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

/**
 * Client-only registry for custom machine OBJ models.
 * Separated from CustomMachineConfigJSON to prevent client-side classes
 * from leaking into server classpaths. Models are loaded lazily on first use.
 */
@SideOnly(Side.CLIENT)
public class MachineModelRegistry {

	/** config name → model path (populated at init, strings only) */
	private static final HashMap<String, String> modelPaths = new HashMap<String, String>();
	/** config name → loaded model (populated on first render, lazy) */
	private static final HashMap<String, IModelCustom> loadedModels = new HashMap<String, IModelCustom>();

	/**
	 * Called from ClientProxy at init: only stores paths, does NOT parse any OBJ files.
	 */
	public static void registerModelPaths() {
		modelPaths.clear();
		loadedModels.clear();
		for(MachineConfiguration config : CustomMachineConfigJSON.niceList) {
			if(config.customModel != null) {
				modelPaths.put(config.unlocalizedName, config.customModel.customModel);
			}
		}
	}

	/**
	 * Called from the renderer: returns cached model if available, otherwise loads and caches it.
	 * Returns null if the config name has no associated model.
	 */
	public static IModelCustom getModel(String configName) {
		IModelCustom cached = loadedModels.get(configName);
		if(cached != null) return cached;

		String path = modelPaths.get(configName);
		if(path == null) return null;

		try {
			IModelCustom model = new HFRWavefrontObject(
				new ResourceLocation(RefStrings.MODID, path)
			).asVBO();
			loadedModels.put(configName, model);
			return model;
		} catch(Exception e) {
			MainRegistry.logger.error("Failed to load custom model '" + path + "' for '" + configName + "'", e);
			return null;
		}
	}

	/** Clears all loaded models (for resource reload support). */
	public static void reload() {
		loadedModels.clear();
	}

	/** Returns the model path for a given config name, or null. */
	public static String getModelPath(String configName) {
		return modelPaths.get(configName);
	}

	/** Returns the model texture path for a given config, or null. */
	public static String getModelTexture(String configName) {
		for(MachineConfiguration config : CustomMachineConfigJSON.niceList) {
			if(config.unlocalizedName.equals(configName) && config.customModel != null) {
				return config.customModel.modelTexture;
			}
		}
		return null;
	}
}
