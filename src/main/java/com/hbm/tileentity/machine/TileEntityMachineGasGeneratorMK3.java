package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerGasGeneratorMK3;
import com.hbm.inventory.gui.GUIMachineGasGeneratorMK3;
import com.hbm.tileentity.IGUIProvider;

import api.hbm.energymk3.VoltageTier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineGasGeneratorMK3 extends TileEntityMK3Provider implements IGUIProvider {

	public int burnTime;
	public int maxBurnTime;
	public long production = 1000;

	public TileEntityMachineGasGeneratorMK3() {
		super(1);
		this.maxPower = 50000;
		this.voltageNominal = VoltageTier.MV.getVoltage();
	}

	@Override
	public String getName() { return "container.gasGeneratorMK3"; }

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(worldObj.getTotalWorldTime() % 20 == 0) this.updateConnections();
			if(burnTime > 0) {
				burnTime--;
				this.power += this.production;
				if(this.power > this.maxPower) this.power = this.maxPower;
			}
			if(this.power > 0) {
				for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
					this.tryProvide(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
			}
			this.networkPackNT(25);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(burnTime);
		buf.writeInt(maxBurnTime);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		burnTime = buf.readInt();
		maxBurnTime = buf.readInt();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.burnTime = nbt.getInteger("burnTime");
		this.maxBurnTime = nbt.getInteger("maxBurnTime");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setInteger("burnTime", burnTime);
		nbt.setInteger("maxBurnTime", maxBurnTime);
	}

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerGasGeneratorMK3(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMachineGasGeneratorMK3(player.inventory, this); }
}
