package com.hbm.util;

import javax.annotation.Nonnull;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import xonin.backhand.api.core.BackhandUtils;

/**
 * Optional integration helpers for Backhand's additional equipment slot.
 */
public final class BackhandCompat {

	public static final String MOD_ID = "backhand";

	private BackhandCompat() { }

	/**
	 * Returns the player's offhand stack when Backhand is installed, {@code null} otherwise.
	 */
	public static ItemStack getOffhandItem(EntityPlayer player) {
		if(player == null || !Loader.isModLoaded(MOD_ID)) return null;
		return getBackhandItem(player);
	}

	@Optional.Method(modid = MOD_ID)
	private static ItemStack getBackhandItem(@Nonnull EntityPlayer player) {
		return BackhandUtils.getOffhandItem(player);
	}
}
