package com.hbm.config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

/** Client-only sound validation. Never call from server-side code. */
@SideOnly(Side.CLIENT)
public class MachineSoundValidator {

	public static void validateSounds() {
		SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
		for(CustomMachineConfigJSON.MachineConfiguration config : CustomMachineConfigJSON.niceList) {
			if(config.progressSound == null || config.progressSound.isEmpty()) continue;

			ResourceLocation soundLocation = new ResourceLocation(config.progressSound);
			SoundEventAccessorComposite accessor = soundHandler.getSound(soundLocation);

			if(accessor == null) {
				CustomMachineConfigJSON.logger.warn("Sound '" + config.progressSound + "' not found for machine '"
					+ config.unlocalizedName + "', disabling progress sound");
				config.progressSound = null;
				CustomMachineConfigJSON.customMachines.put(config.unlocalizedName, config);
			}
		}
	}
}
