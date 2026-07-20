package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerBatteryMK3;
import com.hbm.inventory.gui.GUIMachineBatteryMK3;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk3.IEnergyReceiverMK3;
import api.hbm.energymk3.IEnergyProviderMK3;
import api.hbm.energymk3.VoltageTier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineBatteryMK3 extends TileEntityMachineBase implements IEnergyReceiverMK3, IEnergyProviderMK3, IGUIProvider {

	public long power;
	public long maxPower = 100000;
	public long voltageNominal = VoltageTier.LV.getVoltage();
	public double voltageTolerance = 0.2;

	public TileEntityMachineBatteryMK3() { super(2); }

	@Override
	public String getName() { return "container.batteryMK3"; }

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(worldObj.getTotalWorldTime() % 20 == 0) {
				for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
					this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
					this.tryProvide(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
				}
			}
			this.networkPackNT(25);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
	}

	@Override
	public void setPower(long power) { this.power = power; }
	@Override
	public long getPower() { return this.power; }
	@Override
	public long getMaxPower() { return this.maxPower; }
	@Override
	public long getVoltageNominal() { return this.voltageNominal; }
	@Override
	public double getVoltageTolerance() { return this.voltageTolerance; }
	@Override
	public double getEfficiency() { return 1.0; }
	@Override
	public VoltageTier getVoltageTier() { return VoltageTier.fromVoltage(this.voltageNominal); }

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
	}

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerBatteryMK3(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMachineBatteryMK3(player.inventory, this); }
}
