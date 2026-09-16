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
		if(Loader.isModLoaded(EXPANDED_MOD_ID)) initExpanded();
	}

	@Optional.Method(modid = EXPANDED_MOD_ID)
	private static void initExpanded() {
		BaubleExpandedSlots.tryAssignSlotsUpToMinimum(BaubleExpandedSlots.charmType, 1);
	}

	/**
	 * Returns the player's equipped baubles when Baubles is installed, {@code null} otherwise.
	 */
	public static IInventory getBaubles(EntityPlayer player) {
		if(player == null || !Loader.isModLoaded(MOD_ID)) return null;
		return getBaublesInventory(player);
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

	@Optional.Method(modid = MOD_ID)
	private static IInventory getBaublesInventory(EntityPlayer player) {
		return BaublesApi.getBaubles(player);
	}

	private static final class CombinedInventory implements IInventory {

		private final IInventory primary;
		private final IInventory baubles;

		private CombinedInventory(IInventory primary, IInventory baubles) {
			this.primary = primary;
			this.baubles = baubles;
		}

		private IInventory inventoryFor(int slot) {
			return slot < primary.getSizeInventory() ? primary : baubles;
		}

		private int localSlot(int slot) {
			return slot < primary.getSizeInventory() ? slot : slot - primary.getSizeInventory();
		}

		@Override public int getSizeInventory() { return primary.getSizeInventory() + baubles.getSizeInventory(); }
		@Override public ItemStack getStackInSlot(int slot) { return inventoryFor(slot).getStackInSlot(localSlot(slot)); }
		@Override public ItemStack decrStackSize(int slot, int amount) { return inventoryFor(slot).decrStackSize(localSlot(slot), amount); }
		@Override public ItemStack getStackInSlotOnClosing(int slot) { return inventoryFor(slot).getStackInSlotOnClosing(localSlot(slot)); }
		@Override public void setInventorySlotContents(int slot, ItemStack stack) { inventoryFor(slot).setInventorySlotContents(localSlot(slot), stack); }
		@Override public String getInventoryName() { return primary.getInventoryName(); }
		@Override public boolean hasCustomInventoryName() { return primary.hasCustomInventoryName(); }
		@Override public int getInventoryStackLimit() { return primary.getInventoryStackLimit(); }
		@Override public void markDirty() { primary.markDirty(); baubles.markDirty(); }
		@Override public boolean isUseableByPlayer(EntityPlayer player) { return primary.isUseableByPlayer(player) && baubles.isUseableByPlayer(player); }
		@Override public void openInventory() { primary.openInventory(); baubles.openInventory(); }
		@Override public void closeInventory() { primary.closeInventory(); baubles.closeInventory(); }
		@Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return inventoryFor(slot).isItemValidForSlot(localSlot(slot), stack); }
	}
}
