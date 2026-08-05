package com.hbm.tileentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.network.ConnectorRedWire;
import com.hbm.blocks.network.ConnectorRedWireSuper;
import com.hbm.blocks.network.PylonLarge;
import com.hbm.blocks.network.PylonMedium;
import com.hbm.blocks.network.PylonRedWire;
import com.hbm.blocks.network.Substation;
import com.hbm.util.ColorUtil;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.CableProperties;
import api.hbm.energymk2.IVoltageCableMK2;
import api.hbm.energymk2.Nodespace;
import api.hbm.energymk2.Nodespace.PowerNode;
import api.hbm.energymk2.VoltageEnforcement;
import api.hbm.energymk2.VoltageTier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileEntityPylonBase extends TileEntityCableBaseNT implements IVoltageCableMK2 {

	protected List<int[]> connected = new ArrayList<>();
	public int color;

	private long transferredThisTick;
	private boolean exploded;

	public static int canConnect(TileEntityPylonBase first, TileEntityPylonBase second) {

		if(first.getConnectionType() != second.getConnectionType())
			return 1;

		if(first == second)
			return 2;

		double len = Math.min(first.getMaxWireLength(), second.getMaxWireLength());

		Vec3 firstPos = first.getConnectionPoint();
		Vec3 secondPos = second.getConnectionPoint();

		Vec3 delta = Vec3.createVectorHelper(
				(secondPos.xCoord) - (firstPos.xCoord),
				(secondPos.yCoord) - (firstPos.yCoord),
				(secondPos.zCoord) - (firstPos.zCoord)
				);

		return len >= delta.lengthVector() ? 0 : 3;
	}

	@Override
	public CableProperties getCableProperties() {
		Block block = this.getBlockType();
		if(block instanceof PylonRedWire) return ((PylonRedWire) block).getProperties();
		if(block instanceof PylonMedium) return ((PylonMedium) block).getProperties();
		if(block instanceof PylonLarge) return ((PylonLarge) block).getProperties();
		if(block instanceof Substation) return ((Substation) block).getProperties();
		if(block instanceof ConnectorRedWireSuper) return ((ConnectorRedWireSuper) block).getProperties();
		if(block instanceof ConnectorRedWire) return ((ConnectorRedWire) block).getProperties();
		throw new IllegalStateException("Pylon tile placed without a pylon block");
	}

	@Override
	public void beginPowerTick() {
		transferredThisTick = 0L;
	}

	@Override
	public long getRemainingTransfer() {
		return Math.max(0L, getCableProperties().maxEnergyPerTick - transferredThisTick);
	}

	@Override
	public long useTransferCapacity(long amount) {
		long used = Math.min(Math.max(0L, amount), getRemainingTransfer());
		transferredThisTick += used;
		return used;
	}

	@Override
	public void explodeForWrongVoltage(long suppliedVoltage) {
		if(exploded || worldObj == null || worldObj.isRemote) return;
		if(VoltageEnforcement.isStrict()) {
			exploded = true;
			worldObj.createExplosion(null, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, 2.0F, true);
		} else if(VoltageEnforcement.isWarn()) {
			VoltageEnforcement.warnNearby(this, "hbm.voltage.cableOvervoltageWarn",
					VoltageTier.format(suppliedVoltage), VoltageTier.format(getCableProperties().voltage));
		}
	}

	/** Loss incurred by the wire span leading to the next pylon in a route. Closer pylons lose more per block, longer spans lose less per block. */
	public long getSpanLoss(IVoltageCableMK2 next) {
		if(!(next instanceof TileEntity)) return 0L;
		TileEntity te = (TileEntity) next;
		double dx = te.xCoord - xCoord;
		double dy = te.yCoord - yCoord;
		double dz = te.zCoord - zCoord;
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(distance <= 0D) return 0L;
		return Math.max(1L, Math.round(getCableProperties().lossPerBlock * Math.sqrt(distance)));
	}

	public boolean setColor(ItemStack stack) {
		if(stack == null) return false;
		int color = ColorUtil.getColorFromDye(stack);
		if(color == 0 || color == this.color) return false;
		stack.stackSize--;
		this.color = color;

		this.markDirty();
		if(worldObj instanceof WorldServer) {
			WorldServer world = (WorldServer) worldObj;
			world.getPlayerManager().markBlockForUpdate(xCoord, yCoord, zCoord);
		}

		return true;
	}

	@Override
	public PowerNode createNode() {
		TileEntity tile = (TileEntity) this;
		PowerNode node = new PowerNode(new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord)).setConnections(new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN));
		for(int[] pos : this.connected) node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));
		return node;
	}

	public void addConnection(int x, int y, int z) {

		connected.add(new int[] {x, y, z});

		PowerNode node = Nodespace.getNode(worldObj, xCoord, yCoord, zCoord);
		node.recentlyChanged = true;
		node.addConnection(new DirPos(x, y, z, ForgeDirection.UNKNOWN));

		this.markDirty();

		if(worldObj instanceof WorldServer) {
			WorldServer world = (WorldServer) worldObj;
			world.getPlayerManager().markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	public void disconnectAll() {

		for(int[] pos : connected) {

			TileEntity te = worldObj.getTileEntity(pos[0], pos[1], pos[2]);

			if(te == this)
				continue;

			if(te instanceof TileEntityPylonBase) {
				TileEntityPylonBase pylon = (TileEntityPylonBase) te;
				Nodespace.destroyNode(worldObj, pos[0], pos[1], pos[2]);

				for(int i = 0; i < pylon.connected.size(); i++) {
					int[] conPos = pylon.connected.get(i);

					if(conPos[0] == xCoord && conPos[1] == yCoord && conPos[2] == zCoord) {
						pylon.connected.remove(i);
						i--;
					}
				}

				pylon.markDirty();

				if(worldObj instanceof WorldServer) {
					WorldServer world = (WorldServer) worldObj;
					world.getPlayerManager().markBlockForUpdate(pylon.xCoord, pylon.yCoord, pylon.zCoord);
				}
			}
		}

		Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
	}

	public abstract ConnectionType getConnectionType();
	public abstract Vec3[] getMountPos();
	public abstract double getMaxWireLength();

	public Vec3 getConnectionPoint() {
		Vec3[] mounts = this.getMountPos();

		if(mounts == null || mounts.length == 0)
			return Vec3.createVectorHelper(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);

		return mounts[0].addVector(xCoord, yCoord, zCoord);
	}

	public List<int[]> getConnected() {
		return connected;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setInteger("conCount", connected.size());
		nbt.setInteger("color", color);

		for(int i = 0; i < connected.size(); i++) {
			nbt.setIntArray("con" + i, connected.get(i));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		int count = nbt.getInteger("conCount");
		this.color = nbt.getInteger("color");

		this.connected.clear();

		for(int i = 0; i < count; i++) {
			connected.add(nbt.getIntArray("con" + i));
		}
	}

	@Override
	public Packet getDescriptionPacket() {

		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.func_148857_g());
	}

	public enum ConnectionType {
		SINGLE,
		TRIPLE,
		QUAD
		//more to follow
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
