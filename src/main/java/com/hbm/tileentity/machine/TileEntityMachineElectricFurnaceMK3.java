package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerElectricFurnaceMK3;
import com.hbm.inventory.gui.GUIMachineElectricFurnaceMK3;
import com.hbm.tileentity.IGUIProvider;

import api.hbm.energymk3.VoltageTier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class TileEntityMachineElectricFurnaceMK3 extends TileEntityMK3Consumer implements IGUIProvider {

	public int progress;
	public int maxProgress = 100;
	public int consumption = 50;

	public TileEntityMachineElectricFurnaceMK3() {
		super(3);
		this.maxPower = 10000;
		this.voltageNominal = VoltageTier.LV.getVoltage();
	}

	@Override
	public String getName() { return "container.electricFurnaceMK3"; }

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(worldObj.getTotalWorldTime() % 20 == 0) this.updateConnections();
			if(this.power >= this.consumption && this.canProcess()) {
				this.progress++;
				this.power -= this.consumption;
				if(this.progress >= this.maxProgress) {
					this.progress = 0;
					this.processItem();
				}
			} else {
				this.progress = 0;
			}
			this.networkPackNT(50);
		}
	}

	private boolean canProcess() {
		if(slots[1] == null) return false;
		ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(this.slots[1]);
		if(result == null) return false;
		if(slots[2] == null) return true;
		if(!slots[2].isItemEqual(result)) return false;
		return slots[2].stackSize < slots[2].getMaxStackSize();
	}

	private void processItem() {
		if(canProcess()) {
			ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(this.slots[1]);
			if(slots[2] == null) slots[2] = result.copy();
			else if(slots[2].isItemEqual(result)) slots[2].stackSize += result.stackSize;
			this.decrStackSize(1, 1);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(progress);
		buf.writeInt(maxProgress);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		progress = buf.readInt();
		maxProgress = buf.readInt();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.progress = nbt.getInteger("progress");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setInteger("progress", progress);
	}

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerElectricFurnaceMK3(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMachineElectricFurnaceMK3(player.inventory, this); }
}
