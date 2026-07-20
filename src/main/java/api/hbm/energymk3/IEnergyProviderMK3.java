package api.hbm.energymk3;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toclient.AuxParticlePacketNT;

import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.tile.ILoadedTile.TileAccessCache;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.nbt.NBTTagCompound;

public interface IEnergyProviderMK3 extends IEnergyHandlerMK3 {

	public default void usePower(long power) {
		this.setPower(this.getPower() - power);
	}

	public default long getProviderSpeed() {
		return this.getMaxPower();
	}

	public default void tryProvide(World world, int x, int y, int z, ForgeDirection dir) {

		TileEntity te = TileAccessCache.getTileOrCache(world, x, y, z);

		if(te instanceof IEnergyConductorMK3) {
			IEnergyConductorMK3 con = (IEnergyConductorMK3) te;
			if(con.canConnect(dir.getOpposite())) {

				PowerNodeMK3 node = NodespaceMK3.getNode(world, x, y, z);

				if(node != null && node.net != null) {
					node.net.addProvider(this);
				}
			}
		}

		if(te instanceof IEnergyReceiverMK3 && te != this) {
			IEnergyReceiverMK3 rec = (IEnergyReceiverMK3) te;
			if(rec.canConnect(dir.getOpposite())) {
				long provides = Math.min(this.getPower(), this.getProviderSpeed());
				long receives = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
				long toTransfer = Math.min(provides, receives);
				toTransfer -= rec.transferPower(toTransfer);
				this.usePower(toTransfer);
			}
		}
	}
}
