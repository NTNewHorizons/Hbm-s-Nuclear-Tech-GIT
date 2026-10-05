package com.hbm.tileentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.network.FluidRegulatorValve;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IPersistentNBT;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.storage.TileEntityMachineFluidTank;

import api.hbm.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import api.hbm.fluidmk2.IFluidConnectorMK2;
import api.hbm.fluidmk2.IFluidPipeMK2;
import api.hbm.fluidmk2.IFluidProviderMK2;
import api.hbm.fluidmk2.IFluidReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardSenderMK2;
import api.hbm.fluidmk2.IFluidStandardTransceiverMK2;
import api.ntm1of90.compat.fluid.registry.FluidMappingRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityFluidRegulatorValve extends TileEntityLoadedBase
		implements IFluidReceiverMK2, IFluidStandardSenderMK2, IFluidConnectorMK2, IPersistentNBT, IFluidCopiable {

	public static final int[] LEVELS = new int[] { 1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 99 };
	public int levelIndex = 5; // default 50%
	public FluidType filterType = Fluids.NONE;

	public static final int MAX_RATE = 50_000;

	// Client-side synced data for HUD overlay
	public boolean clientHasTank = false;
	public FluidType clientTankType = Fluids.NONE;
	public int clientTankFill = 0;
	public int clientTankMax = 0;

	public int getTargetPercentage() {
		return LEVELS[MathHelper.clamp_int(levelIndex, 0, LEVELS.length - 1)];
	}

	public void cycleThreshold(boolean backwards) {
		if (backwards) {
			levelIndex = (levelIndex - 1 + LEVELS.length) % LEVELS.length;
		} else {
			levelIndex = (levelIndex + 1) % LEVELS.length;
		}
		worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "hbm:item.screwdriver", 1.0F, 1.0F);
		this.markDirty();
		this.networkPackNT(25);
	}

	public static class TankRef {
		public TileEntity tile;
		public ForgeDirection dir;
		public FluidTank tank;
		public IFluidHandler forgeHandler;
		public FluidType type = Fluids.NONE;
		public int fill = 0;
		public int maxFill = 0;
		public int pressure = 0;
	}

	public List<TankRef> getConnectedTanks() {
		List<TankRef> list = new ArrayList<>();
		if (worldObj == null)
			return list;

		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			TileEntity te = worldObj.getTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
			if (te == null || te == this)
				continue;
			if (te instanceof IFluidPipeMK2 || te instanceof TileEntityFluidRegulatorValve)
				continue;

			// 1. Check for NTM FluidTank
			FluidTank foundTank = null;
			if (te instanceof TileEntityMachineFluidTank) {
				foundTank = ((TileEntityMachineFluidTank) te).tank;
			} else if (te instanceof TileEntityBarrel) {
				foundTank = ((TileEntityBarrel) te).tank;
			} else if (te instanceof IFluidStandardTransceiverMK2) {
				foundTank = selectTank(((IFluidStandardTransceiverMK2) te).getAllTanks());
			} else if (te instanceof IFluidStandardReceiverMK2) {
				foundTank = selectTank(((IFluidStandardReceiverMK2) te).getReceivingTanks());
			} else if (te instanceof IFluidStandardSenderMK2) {
				foundTank = selectTank(((IFluidStandardSenderMK2) te).getSendingTanks());
			}

			if (foundTank != null) {
				TankRef ref = new TankRef();
				ref.tile = te;
				ref.dir = dir;
				ref.tank = foundTank;
				ref.type = foundTank.getTankType();
				ref.fill = foundTank.getFill();
				ref.maxFill = foundTank.getMaxFill();
				ref.pressure = foundTank.getPressure();
				list.add(ref);
				continue;
			}

			// 2. Check for Forge IFluidHandler
			if (te instanceof IFluidHandler) {
				IFluidHandler handler = (IFluidHandler) te;
				FluidTankInfo[] infos = handler.getTankInfo(dir.getOpposite());
				if (infos != null && infos.length > 0) {
					FluidTankInfo info = infos[0];
					if (info != null && info.capacity > 0) {
						TankRef ref = new TankRef();
						ref.tile = te;
						ref.dir = dir;
						ref.forgeHandler = handler;
						if (info.fluid != null && info.fluid.getFluid() != null) {
							ref.type = FluidMappingRegistry.getHbmFluidType(info.fluid.getFluid());
							ref.fill = info.fluid.amount;
						} else {
							ref.type = this.filterType;
							ref.fill = 0;
						}
						ref.maxFill = info.capacity;
						ref.pressure = 0;
						list.add(ref);
					}
				}
			}
		}

		return list;
	}

	public TankRef getTankRef() {
		List<TankRef> list = getConnectedTanks();
		return list.isEmpty() ? null : list.get(0);
	}

	private FluidTank selectTank(FluidTank[] tanks) {
		if (tanks == null || tanks.length == 0)
			return null;
		if (this.filterType != Fluids.NONE) {
			for (FluidTank t : tanks) {
				if (t != null && t.getTankType() == this.filterType)
					return t;
			}
		}
		for (FluidTank t : tanks) {
			if (t != null && t.getFill() > 0)
				return t;
		}
		return tanks[0];
	}

	public FluidType getRegulatedType(TankRef ref) {
		if (this.filterType != Fluids.NONE)
			return this.filterType;
		if (ref != null && ref.type != Fluids.NONE)
			return ref.type;
		return Fluids.NONE;
	}

	@Override
	public void updateEntity() {
		if (!worldObj.isRemote) {
			List<TankRef> tanks = getConnectedTanks();

			// If placed between 2 or more tanks, break itself and drop as an item
			if (tanks.size() >= 2) {
				FluidRegulatorValve.explode(worldObj, xCoord, yCoord, zCoord);
				return;
			}

			if (tanks.size() == 1) {
				TankRef ref = tanks.get(0);
				if (ref.maxFill > 0) {
					FluidType regType = getRegulatedType(ref);
					if (regType != Fluids.NONE) {
						long targetMb = (long) ref.maxFill * getTargetPercentage() / 100L;
						long demand = Math.max(0, targetMb - ref.fill);
						long available = Math.max(0, ref.fill - targetMb);
						int pressure = ref.pressure;

						for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
							if (dir == ref.dir)
								continue;

							int nx = xCoord + dir.offsetX;
							int ny = yCoord + dir.offsetY;
							int nz = zCoord + dir.offsetZ;

							if (demand > 0) {
								this.trySubscribe(regType, worldObj, nx, ny, nz, dir);
							}
							if (available > 0) {
								this.tryProvide(regType, pressure, worldObj, nx, ny, nz, dir);
							}
						}
					}
				}
			}

			if (worldObj.getTotalWorldTime() % 10 == 0) {
				this.networkPackNT(25);
			}
		}
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		TankRef ref = getTankRef();
		if (ref != null && dir == ref.dir) {
			return false; // Isolate the tank from direct pipe network bridging
		}
		FluidType regType = getRegulatedType(ref);
		if (regType == Fluids.NONE)
			return true;
		return type == regType;
	}

	// ===== IFluidReceiverMK2 (Inflow when tank < target) =====

	@Override
	public long getDemand(FluidType type, int pressure) {
		TankRef ref = getTankRef();
		if (ref == null || ref.maxFill <= 0)
			return 0;
		FluidType regType = getRegulatedType(ref);
		if (type != regType || pressure != ref.pressure)
			return 0;

		long targetMb = (long) ref.maxFill * getTargetPercentage() / 100L;
		long demand = Math.max(0, targetMb - ref.fill);
		return Math.min(demand, MAX_RATE);
	}

	@Override
	public long transferFluid(FluidType type, int pressure, long amount) {
		TankRef ref = getTankRef();
		if (ref == null || ref.maxFill <= 0)
			return amount;
		FluidType regType = getRegulatedType(ref);
		if (type != regType || pressure != ref.pressure)
			return amount;

		long targetMb = (long) ref.maxFill * getTargetPercentage() / 100L;
		long needed = Math.max(0, targetMb - ref.fill);
		long toAccept = Math.min(amount, needed);
		if (toAccept <= 0)
			return amount;

		if (ref.tank != null) {
			if (ref.tank.getTankType() == Fluids.NONE) {
				ref.tank.setTankType(regType);
			}
			int accepted = Math.min((int) toAccept, ref.tank.getMaxFill() - ref.tank.getFill());
			ref.tank.setFill(ref.tank.getFill() + accepted);
			ref.tile.markDirty();
			return amount - accepted;
		} else if (ref.forgeHandler != null) {
			Fluid forgeFluid = FluidMappingRegistry.getForgeFluid(regType);
			if (forgeFluid != null) {
				int accepted = ref.forgeHandler.fill(ref.dir.getOpposite(), new FluidStack(forgeFluid, (int) toAccept),
						true);
				return amount - accepted;
			}
		}

		return amount;
	}

	@Override
	public int[] getReceivingPressureRange(FluidType type) {
		TankRef ref = getTankRef();
		if (ref != null) {
			return new int[] { ref.pressure, ref.pressure };
		}
		return DEFAULT_PRESSURE_RANGE;
	}

	@Override
	public ConnectionPriority getFluidPriority() {
		TankRef ref = getTankRef();
		if (ref != null && ref.tile instanceof IFluidReceiverMK2) {
			return ((IFluidReceiverMK2) ref.tile).getFluidPriority();
		}
		return ConnectionPriority.NORMAL;
	}

	@Override
	public long getReceiverSpeed(FluidType type, int pressure) {
		return MAX_RATE;
	}

	// ===== IFluidProviderMK2 (Outflow when tank > target) =====

	@Override
	public FluidTank[] getAllTanks() {
		TankRef ref = getTankRef();
		if (ref != null && ref.tank != null) {
			return new FluidTank[] { ref.tank };
		}
		return new FluidTank[0];
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return new FluidTank[0];
	}

	@Override
	public long getFluidAvailable(FluidType type, int pressure) {
		TankRef ref = getTankRef();
		if (ref == null || ref.maxFill <= 0)
			return 0;
		FluidType regType = getRegulatedType(ref);
		if (type != regType || pressure != ref.pressure)
			return 0;

		long targetMb = (long) ref.maxFill * getTargetPercentage() / 100L;
		long excess = Math.max(0, ref.fill - targetMb);
		return Math.min(excess, MAX_RATE);
	}

	@Override
	public void useUpFluid(FluidType type, int pressure, long amount) {
		TankRef ref = getTankRef();
		if (ref == null || amount <= 0)
			return;

		if (ref.tank != null) {
			int toRemove = Math.min((int) amount, ref.tank.getFill());
			ref.tank.setFill(Math.max(0, ref.tank.getFill() - toRemove));
			ref.tile.markDirty();
		} else if (ref.forgeHandler != null) {
			ref.forgeHandler.drain(ref.dir.getOpposite(), (int) amount, true);
		}
	}

	@Override
	public int[] getProvidingPressureRange(FluidType type) {
		TankRef ref = getTankRef();
		if (ref != null) {
			return new int[] { ref.pressure, ref.pressure };
		}
		return DEFAULT_PRESSURE_RANGE;
	}

	@Override
	public long getProviderSpeed(FluidType type, int pressure) {
		return MAX_RATE;
	}

	// ===== Network Sync & NBT =====

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeByte(this.levelIndex);
		buf.writeInt(this.filterType.getID());

		TankRef ref = getTankRef();
		if (ref != null) {
			buf.writeBoolean(true);
			buf.writeInt(ref.type.getID());
			buf.writeInt(ref.fill);
			buf.writeInt(ref.maxFill);
		} else {
			buf.writeBoolean(false);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.levelIndex = buf.readByte();
		this.filterType = Fluids.fromID(buf.readInt());

		boolean hasTank = buf.readBoolean();
		if (hasTank) {
			this.clientHasTank = true;
			this.clientTankType = Fluids.fromID(buf.readInt());
			this.clientTankFill = buf.readInt();
			this.clientTankMax = buf.readInt();
		} else {
			this.clientHasTank = false;
			this.clientTankType = Fluids.NONE;
			this.clientTankFill = 0;
			this.clientTankMax = 0;
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		writeNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		readNBT(nbt);
	}

	@Override
	public void writeNBT(NBTTagCompound nbt) {
		nbt.setInteger("level", levelIndex);
		nbt.setInteger("type", filterType.getID());
	}

	@Override
	public void readNBT(NBTTagCompound nbt) {
		levelIndex = nbt.getInteger("level");
		filterType = Fluids.fromID(nbt.getInteger("type"));
	}

	// ===== IFluidCopiable =====

	@Override
	public int[] getFluidIDToCopy() {
		return new int[] { filterType.getID() };
	}

	@Override
	public FluidTank getTankToPaste() {
		return null;
	}

	@Override
	public void pasteSettings(NBTTagCompound nbt, int index, World world, EntityPlayer player, int x, int y, int z) {
		int[] ids = nbt.getIntArray("fluidID");
		if (ids.length > 0) {
			int id = index < ids.length ? ids[index] : 0;
			this.filterType = Fluids.fromID(id);
			this.markDirty();
			this.networkPackNT(25);
		}
	}
}
