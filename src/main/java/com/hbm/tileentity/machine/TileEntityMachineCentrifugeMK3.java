package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerCentrifugeMK3;
import com.hbm.inventory.gui.GUIMachineCentrifugeMK3;
import com.hbm.tileentity.IGUIProvider;

import api.hbm.energymk3.VoltageTier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class TileEntityMachineCentrifugeMK3 extends TileEntityMK3Consumer implements IGUIProvider {

	public int progress;
	public int maxProgress = 300;
	public int consumption = 5000;

	public TileEntityMachineCentrifugeMK3() {
		super(6);
		this.maxPower = 500000;
		this.voltageNominal = VoltageTier.HV.getVoltage();
	}

	@Override
	public String getName() { return "container.centrifugeMK3"; }

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(worldObj.getTotalWorldTime() % 20 == 0) this.updateConnections();
			if(this.power >= this.consumption && this.progress < this.maxProgress) {
				this.progress++;
				this.power -= this.consumption;
				if(this.progress >= this.maxProgress) this.progress = 0;
			} else {
				this.progress = 0;
			}
			this.networkPackNT(50);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(progress);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		progress = buf.readInt();
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

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerCentrifugeMK3(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMachineCentrifugeMK3(player.inventory, this); }
}
