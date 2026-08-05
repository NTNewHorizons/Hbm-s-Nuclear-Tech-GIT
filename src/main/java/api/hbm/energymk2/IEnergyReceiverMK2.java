package api.hbm.energymk2;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.interfaces.NotableComments;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.Nodespace.PowerNode;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** If it receives energy, use this */
@NotableComments
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

	public default long getReceiverPower() { return this.getPower(); }
	public default void setReceiverPower(long power) { this.setPower(power); }
	public default long getReceiverMaxPower() { return this.getMaxPower(); }
	public default long getReceiverVoltage() { return this.getVoltage(); }

	Set<IEnergyReceiverMK2> BURNED_OUT = Collections.newSetFromMap(new WeakHashMap<IEnergyReceiverMK2, Boolean>());

	public default long transferPower(long power) {
		if(power + this.getReceiverPower() <= this.getReceiverMaxPower()) {
			this.setReceiverPower(power + this.getReceiverPower());
			return 0;
		}
		long capacity = this.getReceiverMaxPower() - this.getReceiverPower();
		long overshoot = power - capacity;
		this.setReceiverPower(this.getReceiverMaxPower());
		return overshoot;
	}


	public default long transferPowerAtVoltage(long power, long voltage) {
		if(power <= 0) return power;
		if(BURNED_OUT.contains(this)) return power;
		// Legacy safety: an unconfigured (voltage == 0) source must never punish a configured receiver.
		// This keeps pre-existing networks fully functional while the voltage system is active by default.
		if(!VoltageTier.isConfigured(voltage)) return this.transferPower(power);
		if(!VoltageTier.isConfigured(this.getReceiverVoltage())) return this.transferPower(power);
		if(voltage != this.getReceiverVoltage()) {
			this.onOvervoltage(voltage);
			return power;
		}
		return this.transferPower(power);
	}

	public default BlockPos[] getMultiblockPositions() {
		if(this instanceof TileEntity) {
			TileEntity te = (TileEntity) this;
			return new BlockPos[] { new BlockPos(te.xCoord, te.yCoord, te.zCoord) };
		}
		return new BlockPos[0];
	}

	public default void onOvervoltage(long voltage) {
		if(!BURNED_OUT.add(this)) return;
		if(this instanceof TileEntity) {
			TileEntity te = (TileEntity) this;
			World world = te.getWorldObj();
			if(world != null && !world.isRemote) {
				float strength = MachineVoltageRegistry.getExplosionStrength(this);
				for(BlockPos pos : this.getMultiblockPositions()) {
					world.setBlockToAir(pos.getX(), pos.getY(), pos.getZ());
				}
				world.createExplosion(null, te.xCoord + 0.5D, te.yCoord + 0.5D, te.zCoord + 0.5D, strength, true);
			}
		}
	}

	public default long getReceiverSpeed() {
		return this.getReceiverMaxPower();
	}

	/** Whether a provider can provide power by touching the block (i.e. via proxies), bypassing the need for a network entirely */
	public default boolean allowDirectProvision() { return true; }

	public default void trySubscribe(World world, DirPos pos) { trySubscribe(world, pos.getX(), pos.getY(), pos.getZ(), pos.getDir()); }

	public default void trySubscribe(World world, int x, int y, int z, ForgeDirection dir) {

		TileEntity te = TileAccessCache.getTileOrCache(world, x, y, z);
		boolean red = false;

		if(te instanceof IEnergyConductorMK2) {
			IEnergyConductorMK2 con = (IEnergyConductorMK2) te;
			if(!con.canConnect(dir.getOpposite())) return;

			PowerNode node = Nodespace.getNode(world, x, y, z);

			if(node != null && node.net != null) {
				node.net.addReceiverAt(this, x, y, z, dir.getOpposite());
				red = true;
			}
		}

		if(particleDebug) {
			NBTTagCompound data = new NBTTagCompound();
			data.setString("type", "network");
			data.setString("mode", "power");
			double posX = x + 0.5 + dir.offsetX * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posY = y + 0.5 + dir.offsetY * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posZ = z + 0.5 + dir.offsetZ * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			data.setDouble("mX", -dir.offsetX * (red ? 0.025 : 0.1));
			data.setDouble("mY", -dir.offsetY * (red ? 0.025 : 0.1));
			data.setDouble("mZ", -dir.offsetZ * (red ? 0.025 : 0.1));
			PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, posX, posY, posZ), new TargetPoint(world.provider.dimensionId, posX, posY, posZ, 25));
		}
	}

	public default void tryUnsubscribe(World world, int x, int y, int z) {

		TileEntity te = world.getTileEntity(x, y, z);

		if(te instanceof IEnergyConductorMK2) {
			IEnergyConductorMK2 con = (IEnergyConductorMK2) te;
			PowerNode node = con.createNode();

			if(node != null && node.net != null) {
				node.net.removeReceiver(this);
			}
		}
	}

	public enum ConnectionPriority {
		LOWEST,
		LOW,
		NORMAL,
		HIGH,
		HIGHEST
	}

	public default ConnectionPriority getPriority() {
		return ConnectionPriority.NORMAL;
	}
}
