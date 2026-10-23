package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;

import api.hbm.energymk2.IEnergyProviderMK2;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityChungusSolar extends TileEntityLoadedBase implements IEnergyProviderMK2 {

	/** How many regular solar panel sections the array represents; the model spans 5 sections over 18 blocks */
	public static final int ACCUMULATED_PANELS = 5;
	/** The array's cells are gold laminated, so they are +50% more efficient than a regular panel */
	public static final double PANEL_BONUS_EFFICIENCY = 1.5D;
	/** A regular panel buffers 1,000 HE, this machine buffers 5 times that */
	public static final long MAX_POWER = 5_000;

	private long power;
	private long maxpwr = MAX_POWER;

	protected ModulePortManPower modulePowerPorts;

	public TileEntityChungusSolar() {
		modulePowerPorts = new ModulePortManPower(this).onlyOutput();
	}

	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			// single port at the cap end, connecting only through the port face
			cachedPorts = new PortDef[] {
					PortDef.make(xCoord, yCoord, zCoord, ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset))
			};
		}
		return cachedPorts;
	}

	@Override
	public void updateEntity() {

		if(!worldObj.isRemote) {
			modulePowerPorts.update(getPorts());

			// Sun power ranges from 1-4
			int sun = worldObj.getSavedLightValue(EnumSkyBlock.Sky, xCoord, yCoord, zCoord) - worldObj.skylightSubtracted - 11;

			if(sun > 0 && worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord)) {
				power += getOutput(sun);

				if(power > getMaxPower())
					power = getMaxPower();
			}

			networkPackNT(50);
		}
	}

	// Balanced around 750HE/t on Earth, i.e. 7.5 times one regular solar panel (100HE/t): 5 sections at +50% efficiency
	public long getOutput(int sun) {
		return MathHelper.ceiling_float_int((float) (sun * 25 * ACCUMULATED_PANELS * PANEL_BONUS_EFFICIENCY * WorldProviderCelestial.getSunPower(worldObj.provider, xCoord, zCoord)));
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getMaxPower() {
		return maxpwr;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		// the port is the only connection point, all other faces of the array are inaccessible
		return dir == ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(power);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.power = nbt.getLong("power");
		if(nbt.hasKey("maxpwr")) this.maxpwr = nbt.getLong("maxpwr");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setLong("power", power);
		nbt.setLong("maxpwr", maxpwr);
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();

		if(modulePowerPorts != null) modulePowerPorts.destroy();
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(modulePowerPorts != null) modulePowerPorts.destroy();
	}

	@Override
	public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
		return net.minecraft.tileentity.TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
