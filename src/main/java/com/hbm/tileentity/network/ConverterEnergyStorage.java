package com.hbm.tileentity.network;

import cofh.api.energy.EnergyStorage;
import net.minecraft.nbt.NBTTagCompound;

/** A configurable RF buffer that retains excess energy until it can be drained. */
class ConverterEnergyStorage extends EnergyStorage {

	public ConverterEnergyStorage(int capacity) {
		super(capacity, capacity, capacity);
	}

	@Override
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public void setEnergyStored(int energy) {
		this.energy = Math.max(0, energy);
	}

	@Override
	public EnergyStorage readFromNBT(NBTTagCompound nbt) {
		setEnergyStored(nbt.getInteger("Energy"));
		return this;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		if(energy >= capacity) return 0;
		return super.receiveEnergy(maxReceive, simulate);
	}
}
