package com.hbm.util;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Optional integration helpers for Baubles' equipped item inventory.
 */
public final class BaublesCompat {

	public static final String MOD_ID = "Baubles";
	public static final String EXPANDED_MOD_ID = "Baubles|Expanded";

	private BaublesCompat() { }

	/**
	 * Requests the additional slot types used by NTM items from Baubles Expanded.
	 * Expanded only accepts slot assignments during pre-initialization.
	 */
	public static void init() {
		if(Loader.isModLoaded(EXPANDED_MOD_ID)) _init();
	}

	@Optional.Method(modid = EXPANDED_MOD_ID)
	private static void _init() {
		BaubleExpandedSlots.tryAssignSlotsUpToMinimum(BaubleExpandedSlots.charmType, 1);
	}

	/**
	 * Returns the player's equipped baubles when Baubles is installed, {@code null} otherwise.
	 */
	public static IInventory getBaubles(EntityPlayer player) {
		if(player == null || !Loader.isModLoaded(MOD_ID)) return null;
		return _getBaubles(player);
	}

	@Optional.Method(modid = MOD_ID)
	private static IInventory _getBaubles(EntityPlayer player) {
		return BaublesApi.getBaubles(player);
	}

	/**
	 * Returns whether the player has the item equipped in a bauble slot.
	 */
	public static boolean hasItem(EntityPlayer player, Item item) {
		IInventory baubles = getBaubles(player);
		if(baubles == null || item == null) return false;

		for(int i = 0; i < baubles.getSizeInventory(); i++) {
			if(baubles.getStackInSlot(i) != null && baubles.getStackInSlot(i).getItem() == item) return true;
		}

		return false;
	}

	/**
	 * Includes equipped baubles when code scans a player's inventory.
	 */
	public static IInventory includeBaubles(IInventory inventory) {
		if(inventory instanceof CombinedInventory || !(inventory instanceof InventoryPlayer)) return inventory;

		IInventory baubles = getBaubles(((InventoryPlayer) inventory).player);
		return baubles == null ? inventory : new CombinedInventory(inventory, baubles);
	}

	/**
	 * Returns a stable slot index after the vanilla inventory for an equipped bauble stack.
	 */
	public static int getCombinedSlot(EntityPlayer player, ItemStack stack) {
		IInventory baubles = getBaubles(player);
		if(baubles != null) {
			for(int i = 0; i < baubles.getSizeInventory(); i++) {
				if(baubles.getStackInSlot(i) == stack) return player.inventory.getSizeInventory() + i;
			}
		}

		return player.inventory.currentItem;
	}
}
