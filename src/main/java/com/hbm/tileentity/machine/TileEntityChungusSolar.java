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
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityChungusSolar extends TileEntityLoadedBase implements IEnergyProviderMK2 {

	public static final int ACCUMULATED_PANELS = 9;
	public static final double PANEL_BONUS_EFFICIENCY = 1.5D;
	public static final long MAX_POWER = 5_000;
	public static final int EXPOSURE_UPDATE_INTERVAL_TICKS = 10;

	private long power;
	private long maxpwr = MAX_POWER;
	public int panelRotation;
	private List<BlockPos> exposurePositions;
	private int cachedPanelRotation = -1;
	private int cachedFacingMetadata = -1;
	private long cachedExposedOutput;
	private long lastExposureUpdate = -1;
	private AxisAlignedBB renderBounds;
	private int renderBoundsMetadata = -1;

	protected ModulePortManPower modulePowerPorts;

	public TileEntityChungusSolar() {
		modulePowerPorts = new ModulePortManPower(this).onlyOutput();
	}

	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			cachedPorts = new PortDef[] {
					PortDef.make(xCoord, yCoord, zCoord, ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset))
			};
		}
		return cachedPorts;
	}

	@Override
	public void updateEntity() {

		if(!worldObj.isRemote) {
			if(worldObj.getTotalWorldTime() % 20 == 0) {
				((MachineChungusSolar) getBlockType()).validateStructure(this);
				if(isInvalid()) return;
			}
			modulePowerPorts.update(getPorts());

			setPower(Math.min(getMaxPower(), power + getCachedExposedOutput()));

			networkPackNT(50);
		}
	}

	private long getCachedExposedOutput() {
		long time = worldObj.getTotalWorldTime();
		// Amortize the cell lighting queries; a pose change must not reuse the old exposure.
		if(lastExposureUpdate < 0 || time < lastExposureUpdate
				|| time - lastExposureUpdate >= EXPOSURE_UPDATE_INTERVAL_TICKS
				|| cachedPanelRotation != panelRotation || cachedFacingMetadata != getBlockMetadata()) {
			cachedExposedOutput = getExposedOutput();
			lastExposureUpdate = time;
		}
		return cachedExposedOutput;
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
		this.panelRotation = buf.readUnsignedByte() % MachineChungusSolar.PANEL_ROTATIONS;
	}

	@Override
	public Packet getDescriptionPacket() {
		// Rotation must reach every chunk watcher, including clients beyond the power update radius.
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("panelRotation", (byte) panelRotation);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		this.panelRotation = packet.func_148857_g().getByte("panelRotation") & (MachineChungusSolar.PANEL_ROTATIONS - 1);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.power = nbt.getLong("power");
		this.panelRotation = nbt.getByte("panelRotation") & (MachineChungusSolar.PANEL_ROTATIONS - 1);
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
	public AxisAlignedBB getRenderBoundingBox() {
		// The panel rotates about y + 0.5 with radius < 1.6. Include every facing and pose.
		int metadata = getBlockMetadata();
		ForgeDirection facing = ForgeDirection.getOrientation(metadata - BlockDummyable.offset);
		int endX = xCoord - facing.offsetX * MachineChungusSolar.PANEL_LENGTH;
		int endZ = zCoord - facing.offsetZ * MachineChungusSolar.PANEL_LENGTH;
		int minX = Math.min(xCoord, endX) - 2, minZ = Math.min(zCoord, endZ) - 2;
		if(renderBounds == null || renderBoundsMetadata != metadata || renderBounds.minY != yCoord - 2
				|| renderBounds.minX != minX || renderBounds.minZ != minZ) {
			renderBounds = AxisAlignedBB.getBoundingBox(minX, yCoord - 2, minZ, Math.max(xCoord, endX) + 3,
					yCoord + 3, Math.max(zCoord, endZ) + 3);
			renderBoundsMetadata = metadata;
		}
		return renderBounds;
	}

	@Override
	@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
