package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityMachineChemDispenser;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/** The dispenser intentionally has no inventory slots or beaker slot. */
public class ContainerChemDispenser extends Container {

	private final TileEntityMachineChemDispenser dispenser;

	public ContainerChemDispenser(TileEntityMachineChemDispenser dispenser) {
		this.dispenser = dispenser;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return dispenser.isUseableByPlayer(player);
	}
}
