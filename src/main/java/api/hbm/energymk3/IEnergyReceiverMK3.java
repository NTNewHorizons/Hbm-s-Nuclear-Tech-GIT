package api.hbm.energymk3;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.tile.ILoadedTile.TileAccessCache;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.nbt.NBTTagCompound;

public interface IEnergyReceiverMK3 extends IEnergyHandlerMK3 {

	public default long transferPower(long power) {
		if(power + this.getPower() <= this.getMaxPower()) {
			this.setPower(power + this.getPower());
			return 0;
		}
		long capacity = this.getMaxPower() - this.getPower();
		long overshoot = power - capacity;
		this.setPower(this.getMaxPower());
		return overshoot;
	}

	public default long getReceiverSpeed() {
		return this.getMaxPower();
	}

	public default void onOvervoltage(long voltageReceived, long voltageNominal) {}
	public default void onUndervoltage(long voltageReceived, long voltageNominal) {}

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

	public default void trySubscribe(World world, DirPos pos) { trySubscribe(world, pos.getX(), pos.getY(), pos.getZ(), pos.getDir()); }

	public default void trySubscribe(World world, int x, int y, int z, ForgeDirection dir) {

		TileEntity te = TileAccessCache.getTileOrCache(world, x, y, z);

		if(te instanceof IEnergyConductorMK3) {
			IEnergyConductorMK3 con = (IEnergyConductorMK3) te;
			if(!con.canConnect(dir.getOpposite())) return;

			PowerNodeMK3 node = NodespaceMK3.getNode(world, x, y, z);

			if(node != null && node.net != null) {
				node.net.addReceiver(this);
			}
		}
	}

	public default void tryUnsubscribe(World world, int x, int y, int z) {

		TileEntity te = world.getTileEntity(x, y, z);

		if(te instanceof IEnergyConductorMK3) {
			IEnergyConductorMK3 con = (IEnergyConductorMK3) te;
			PowerNodeMK3 node = con.createNode();

			if(node != null && node.net != null) {
				node.net.removeReceiver(this);
			}
		}
	}
}
