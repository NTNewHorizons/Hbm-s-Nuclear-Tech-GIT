package com.hbm.tileentity.network;

import java.util.ArrayList;
import java.util.List;

import api.hbm.energymk2.CableProperties;
import api.hbm.energymk2.IVoltageCableMK2;
import api.hbm.energymk2.Nodespace;
import api.hbm.energymk2.Nodespace.PowerNode;
import com.hbm.blocks.network.BlockVoltageCable;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityVoltageCable extends TileEntityCableBaseNT implements IVoltageCableMK2 {

	private long transferredThisTick;
	private boolean exploded;
	private byte disconnectedSides;
	private byte forcedOpenSides;
	private byte pendingAirCutResets;
	private boolean pendingNeighbourRebuild;

	@Override
	public CableProperties getCableProperties() {
		if(this.getBlockType() instanceof BlockVoltageCable) {
			return ((BlockVoltageCable) this.getBlockType()).getProperties();
		}
		throw new IllegalStateException("Voltage cable tile placed without a voltage cable block");
	}

	@Override
	public boolean canConnect(ForgeDirection direction) {
		if(!isConnectionEnabled(direction) || worldObj == null) return false;
		TileEntity neighbour = worldObj.getTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
		if(neighbour instanceof TileEntityVoltageCable) {
			TileEntityVoltageCable cable = (TileEntityVoltageCable) neighbour;
			return cable.isConnectionEnabled(direction.getOpposite())
					&& cable.getCableProperties().voltage == getCableProperties().voltage;
		}
		if(neighbour instanceof TileEntityVoltageTransformer) {
			return ((TileEntityVoltageTransformer) neighbour).canConnectCable(direction.getOpposite(), getCableProperties().voltage);
		}
		if(neighbour instanceof TileEntityPylonBase) {
			TileEntityPylonBase pylon = (TileEntityPylonBase) neighbour;
			return pylon.canConnect(direction.getOpposite())
					&& pylon.getCableProperties().voltage == getCableProperties().voltage;
		}
		if(neighbour instanceof TileEntityProxyBase) {
			TileEntity core = ((TileEntityProxyBase) neighbour).getTE();
			if(core instanceof TileEntityPylonBase) {
				TileEntityPylonBase pylon = (TileEntityPylonBase) core;
				return pylon.canConnect(direction.getOpposite())
						&& pylon.getCableProperties().voltage == getCableProperties().voltage;
			}
		}
		if(neighbour instanceof TileEntityCableBaseNT) return false;
		return true;
	}

	public boolean isConnectionEnabled(ForgeDirection direction) {
		if(direction == null || direction == ForgeDirection.UNKNOWN) return false;
		return (disconnectedSides & (1 << direction.ordinal())) == 0;
	}

	@Override
	public boolean isFaceConnected(ForgeDirection direction) {
		return isConnectionEnabled(direction);
	}

	public boolean isConnectionForced(ForgeDirection direction) {
		if(direction == null || direction == ForgeDirection.UNKNOWN || worldObj == null) return false;
		int bit = 1 << direction.ordinal();
		return (forcedOpenSides & bit) != 0;
	}

	public boolean isConnected(ForgeDirection direction) {
		if(!isConnectionEnabled(direction)) return false;
		if(canConnect(direction)
				&& Library.canConnect(worldObj, xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, direction)) return true;
		return isConnectionForced(direction);
	}

	public boolean toggleConnection(ForgeDirection direction) {
		if(direction == null || direction == ForgeDirection.UNKNOWN || worldObj == null || worldObj.isRemote) return false;
		int bit = 1 << direction.ordinal();
		boolean mirrorCut = false;
		boolean mirrorReconnect = false;
		if((forcedOpenSides & bit) != 0) {
			forcedOpenSides = (byte) (forcedOpenSides & ~bit);
			updateMatchingCableOpenEnd(direction, false);
			if(hasAutomaticConnection(direction)) {
				addCut(direction);
				mirrorCut = true;
			}
		} else if((disconnectedSides & bit) != 0) {
			removeCut(direction);
			mirrorReconnect = true;
		} else if(hasAutomaticConnection(direction)) {
			addCut(direction);
			mirrorCut = true;
		} else {
			forcedOpenSides |= (byte) bit;
			updateMatchingCableOpenEnd(direction, true);
		}
		if(mirrorCut) updateMatchingCableCut(direction, true);
		if(mirrorReconnect) updateMatchingCableCut(direction, false);
		rebuildPowerNode();
		markDirty();
		refreshCableRender();
		return true;
	}

	private void updateMatchingCableOpenEnd(ForgeDirection direction, boolean open) {
		TileEntity neighbour = worldObj.getTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
		if(!(neighbour instanceof TileEntityVoltageCable)) return;
		TileEntityVoltageCable cable = (TileEntityVoltageCable) neighbour;
		int bit = 1 << direction.getOpposite().ordinal();
		if(open) cable.forcedOpenSides |= (byte) bit;
		else cable.forcedOpenSides = (byte) (cable.forcedOpenSides & ~bit);
		cable.markDirty();
		cable.refreshCableRender();
	}

	private void updateMatchingCableCut(ForgeDirection direction, boolean cut) {
		TileEntity neighbour = worldObj.getTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
		if(!(neighbour instanceof TileEntityVoltageCable)) return;
		TileEntityVoltageCable cable = (TileEntityVoltageCable) neighbour;
		if(cable.getCableProperties().voltage != getCableProperties().voltage) return;

		ForgeDirection opposite = direction.getOpposite();
		if(cut) {
			cable.forcedOpenSides = (byte) (cable.forcedOpenSides & ~(1 << opposite.ordinal()));
			cable.addCut(opposite);
		} else {
			cable.removeCut(opposite);
		}
		cable.rebuildPowerNode();
		cable.markDirty();
		cable.refreshCableRender();
	}

	private void addCut(ForgeDirection direction) {
		disconnectedSides |= (byte) (1 << direction.ordinal());
	}

	private void removeCut(ForgeDirection direction) {
		disconnectedSides = (byte) (disconnectedSides & ~(1 << direction.ordinal()));
	}

	private boolean hasAutomaticConnection(ForgeDirection direction) {
		return canConnect(direction)
				&& Library.canConnect(worldObj, xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, direction);
	}

	public void clearCutsForChangedNeighbours() {
		if(worldObj == null || worldObj.isRemote || disconnectedSides == 0) return;
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			int bit = 1 << direction.ordinal();
			if((disconnectedSides & bit) == 0) continue;
			int x = xCoord + direction.offsetX;
			int y = yCoord + direction.offsetY;
			int z = zCoord + direction.offsetZ;
			if(worldObj.blockExists(x, y, z) && worldObj.isAirBlock(x, y, z)) {
				pendingAirCutResets |= (byte) bit;
			} else {
				pendingAirCutResets = (byte) (pendingAirCutResets & ~bit);
			}
		}
	}

	@Override
	public void updateEntity() {
		if(worldObj != null && !worldObj.isRemote && pendingNeighbourRebuild) {
			pendingNeighbourRebuild = false;
			rebuildPowerNode();
		}
		super.updateEntity();
		if(worldObj == null || worldObj.isRemote || pendingAirCutResets == 0) return;

		byte pending = pendingAirCutResets;
		pendingAirCutResets = 0;
		boolean changed = false;
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			int bit = 1 << direction.ordinal();
			if((pending & bit) == 0 || (disconnectedSides & bit) == 0) continue;
			int x = xCoord + direction.offsetX;
			int y = yCoord + direction.offsetY;
			int z = zCoord + direction.offsetZ;
			if(worldObj.blockExists(x, y, z) && worldObj.isAirBlock(x, y, z)) {
				removeCut(direction);
				changed = true;
			}
		}
		if(changed) {
			rebuildPowerNode();
			markDirty();
			refreshCableRender();
		}
	}

	@Override
	public PowerNode createNode() {
		List<DirPos> connections = new ArrayList<DirPos>();
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if(!isConnectionEnabled(direction)) continue;
			TileEntity neighbour = worldObj.getTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
			if(neighbour instanceof TileEntityVoltageCable) {
				TileEntityVoltageCable cable = (TileEntityVoltageCable) neighbour;
				if(cable.isConnectionEnabled(direction.getOpposite())
						&& cable.getCableProperties().voltage == getCableProperties().voltage) {
					connections.add(new DirPos(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, direction));
				}
			} else if(neighbour instanceof TileEntityVoltageTransformer) {
				TileEntityVoltageTransformer transformer = (TileEntityVoltageTransformer) neighbour;
				if(transformer.canConnectCable(direction.getOpposite(), getCableProperties().voltage)) {
					connections.add(new DirPos(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, direction));
				}
			} else if(neighbour instanceof TileEntityPylonBase) {
				TileEntityPylonBase pylon = (TileEntityPylonBase) neighbour;
				if(pylon.canConnect(direction.getOpposite())
						&& pylon.getCableProperties().voltage == getCableProperties().voltage) {
					connections.add(new DirPos(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, direction));
				}
			} else if(neighbour instanceof TileEntityProxyBase) {
				TileEntity core = ((TileEntityProxyBase) neighbour).getTE();
				if(core instanceof TileEntityPylonBase) {
					TileEntityPylonBase pylon = (TileEntityPylonBase) core;
					if(pylon.canConnect(direction.getOpposite())
							&& pylon.getCableProperties().voltage == getCableProperties().voltage) {
						connections.add(new DirPos(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, direction));
					}
				}
			}
		}
		return new PowerNode(new BlockPos(xCoord, yCoord, zCoord)).setConnections(connections.toArray(new DirPos[connections.size()]));
	}

	/** Marks the node for a rebuild on the next tick so connections are recomputed against the current neighbours. */
	public void scheduleNeighbourRebuild() {
		pendingNeighbourRebuild = true;
	}

	private void rebuildPowerNode() {
		if(node != null) {
			Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
			node = null;
		}
	}

	private void refreshCableRender() {
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			worldObj.markBlockForUpdate(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
		}
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
		if(used > 0L && explodeForDifferentVoltageContact()) return 0L;
		return used;
	}

	private boolean explodeForDifferentVoltageContact() {
		CableProperties own = getCableProperties();
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if(!isConnectionEnabled(direction) || !isConnectionForced(direction)) continue;
			TileEntity neighbour = worldObj.getTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
			if(!(neighbour instanceof TileEntityVoltageCable)) continue;
			TileEntityVoltageCable cable = (TileEntityVoltageCable) neighbour;
			CableProperties other = cable.getCableProperties();
			if(!cable.isConnectionEnabled(direction.getOpposite()) || !cable.isConnectionForced(direction.getOpposite()) || own.voltage == other.voltage) continue;
			cable.explodeForWrongVoltage(own.voltage);
			explodeForWrongVoltage(other.voltage);
			return true;
		}
		return false;
	}

	@Override
	public void explodeForWrongVoltage(long suppliedVoltage) {
		if(exploded || worldObj == null || worldObj.isRemote) return;
		exploded = true;
		worldObj.createExplosion(null, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, 2.0F, true);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		disconnectedSides = nbt.getByte("voltageCableDisconnectedSides");
		forcedOpenSides = nbt.getByte("voltageCableForcedOpenSides");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("voltageCableDisconnectedSides", disconnectedSides);
		nbt.setByte("voltageCableForcedOpenSides", forcedOpenSides);
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		readFromNBT(packet.func_148857_g());
		if(worldObj != null) refreshCableRender();
	}

}
