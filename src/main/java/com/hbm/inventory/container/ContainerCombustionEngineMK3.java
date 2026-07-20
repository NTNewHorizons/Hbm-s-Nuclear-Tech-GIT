package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityMachineCombustionEngineMK3;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCombustionEngineMK3 extends Container {

	private TileEntityMachineCombustionEngineMK3 te;

	public ContainerCombustionEngineMK3(InventoryPlayer invPlayer, TileEntityMachineCombustionEngineMK3 te) {
		this.te = te;
		this.addSlotToContainer(new Slot(te, 0, 44, 35));
		this.addSlotToContainer(new Slot(te, 1, 80, 35));

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
		for(int i = 0; i < 9; i++)
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142));
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack rStack = null;
		Slot slot = (Slot) this.inventorySlots.get(index);
		if(slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			rStack = stack.copy();
			if(index <= 1) {
				if(!this.mergeItemStack(stack, 2, this.inventorySlots.size(), true)) return null;
			} else if(!this.mergeItemStack(stack, 0, 2, false)) return null;
			if(stack.stackSize == 0) slot.putStack((ItemStack) null);
			else slot.onSlotChanged();
		}
		return rStack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return te.isUseableByPlayer(player);
	}
}
