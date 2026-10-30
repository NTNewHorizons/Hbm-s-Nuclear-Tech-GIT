package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineChungusSolar;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.fauxpointtwelve.BlockPos;

import api.hbm.energymk2.IEnergyProviderMK2;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityChungusSolar extends TileEntityLoadedBase implements IEnergyProviderMK2 {

	public static final int ACCUMULATED_PANELS = 9;
	public static final double PANEL_BONUS_EFFICIENCY = 1.5D;
	public static final long MAX_POWER = 5_000;

	private long power;
	private long maxpwr = MAX_POWER;
	public int panelRotation;
	private boolean footprintUpdated;
	private List<BlockPos> exposurePositions;
	private int cachedPanelRotation = -1;
	private int cachedFacingMetadata = -1;

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
			if(!footprintUpdated) footprintUpdated = ((MachineChungusSolar) getBlockType()).updateLegacyFootprint(this);
			modulePowerPorts.update(getPorts());

			setPower(Math.min(getMaxPower(), power + getExposedOutput()));

			networkPackNT(50);
		}
	}

	private long getExposedOutput() {
		List<BlockPos> positions = getExposurePositions();
		long output = 0;
		for(BlockPos pos : positions) {
			int x = pos.getX(), y = pos.getY(), z = pos.getZ();
			// Do not load neighboring chunks to generate power.
			if(!worldObj.blockExists(x, y - 1, z)) continue;
			if(!worldObj.canBlockSeeTheSky(x, y, z)) continue;
			int sun = worldObj.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) - worldObj.skylightSubtracted - 11;
			if(sun > 0) output += getOutput(sun);
		}
		return output / positions.size();
	}

	private List<BlockPos> getExposurePositions() {
		int facingMetadata = getBlockMetadata();
		if(exposurePositions == null || cachedPanelRotation != panelRotation || cachedFacingMetadata != facingMetadata) {
			List<BlockPos> cells = ((MachineChungusSolar) getBlockType()).getSolarCellBlocks(this);
			Set<BlockPos> cellSet = new HashSet<>(cells);
			exposurePositions = new ArrayList<>(cells.size());
			for(BlockPos cell : cells) {
				int x = cell.getX(), y = cell.getY() + 1, z = cell.getZ();
				// A vertical array shares columns with itself; ignore its own cells above this one.
				while(cellSet.contains(new BlockPos(x, y, z))) y++;
				exposurePositions.add(new BlockPos(x, y, z));
			}
			cachedPanelRotation = panelRotation;
			cachedFacingMetadata = facingMetadata;
		}
		return exposurePositions;
	}

	// 1,350 HE/t on Earth: nine regular panels (9 x 100 HE/t) at +50% efficiency
	public long getOutput(int sun) {
		return MathHelper.ceiling_float_int((float) (sun * 25 * ACCUMULATED_PANELS * PANEL_BONUS_EFFICIENCY * WorldProviderCelestial.getSunPower(worldObj.provider, xCoord, zCoord)));
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long power) {
		if(this.power != power) {
			this.power = power;
			if(worldObj != null && !worldObj.isRemote) markChanged();
		}
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
		buf.writeByte(panelRotation);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
		this.panelRotation = buf.readUnsignedByte() % 8;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.power = nbt.getLong("power");
		this.panelRotation = (nbt.getByte("panelRotation") & 7);
		if(nbt.hasKey("maxpwr")) this.maxpwr = nbt.getLong("maxpwr");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setLong("power", power);
		nbt.setByte("panelRotation", (byte) panelRotation);
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
