package api.hbm.energymk2;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toclient.AuxParticlePacketNT;

import api.hbm.energymk2.Nodespace.PowerNode;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** If it sends energy, use this */
public interface IEnergyProviderMK2 extends IEnergyHandlerMK2 {

	public default long getProviderPower() { return this.getPower(); }
	public default void setProviderPower(long power) { this.setPower(power); }
	public default long getProviderMaxPower() { return this.getMaxPower(); }
	public default long getProviderVoltage() { return this.getVoltage(); }

	/** Uses up available power, default implementation has no sanity checking, make sure that the requested power is lequal to the current power */
	public default void usePower(long power) {
		this.setProviderPower(this.getProviderPower() - power);
	}

	public default long getProviderSpeed() {
		return this.getProviderMaxPower();
	}

	public default void tryProvide(World world, int x, int y, int z, ForgeDirection dir) {

		TileEntity te = TileAccessCache.getTileOrCache(world, x, y, z);
		boolean red = false;

		if(te instanceof IEnergyConductorMK2) {
			IEnergyConductorMK2 con = (IEnergyConductorMK2) te;
			if(con.canConnect(dir.getOpposite())) {

				PowerNode node = Nodespace.getNode(world, x, y, z);

				if(node != null && node.net != null) {
					node.net.addProviderAt(this, x, y, z, dir.getOpposite());
					red = true;
				}
			}
		}

		if(te instanceof IEnergyReceiverMK2 && te != this) {
			IEnergyReceiverMK2 rec = (IEnergyReceiverMK2) te;
			if(rec.canConnect(dir.getOpposite()) && rec.allowDirectProvision()) {
				long provides = Math.min(this.getProviderPower(), this.getProviderSpeed());
				long receives = Math.min(rec.getReceiverMaxPower() - rec.getReceiverPower(), rec.getReceiverSpeed());
				long toTransfer = Math.min(provides, receives);
				toTransfer -= rec.transferPowerAtVoltage(toTransfer, this.getProviderVoltage());
				this.usePower(toTransfer);
			}
		}

		if(particleDebug) {
			NBTTagCompound data = new NBTTagCompound();
			data.setString("type", "network");
			data.setString("mode", "power");
			double posX = x + 0.5 - dir.offsetX * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posY = y + 0.5 - dir.offsetY * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posZ = z + 0.5 - dir.offsetZ * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			data.setDouble("mX", dir.offsetX * (red ? 0.025 : 0.1));
			data.setDouble("mY", dir.offsetY * (red ? 0.025 : 0.1));
			data.setDouble("mZ", dir.offsetZ * (red ? 0.025 : 0.1));
			PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, posX, posY, posZ), new TargetPoint(world.provider.dimensionId, posX, posY, posZ, 25));
		}
	}
}
