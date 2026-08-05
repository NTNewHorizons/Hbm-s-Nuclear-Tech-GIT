package api.hbm.energymk2;

import com.hbm.util.CompatEnergyControl;

import api.hbm.tile.ILoadedTile;
import com.hbm.util.fauxpointtwelve.BlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

/** DO NOT USE DIRECTLY! This is simply the common ancestor to providers and receivers, because all this behavior has to be excluded from conductors! */
public interface IEnergyHandlerMK2 extends IEnergyConnectorMK2, ILoadedTile {

	public long getPower();
	public void setPower(long power);
	public long getMaxPower();

	public default long getVoltage() {
		return MachineVoltageRegistry.getVoltage(this);
	}

	public default BlockPos[] getVoltageConnectionPoints() {
		if(this instanceof TileEntity) {
			TileEntity tile = (TileEntity) this;
			return new BlockPos[] { new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord) };
		}
		return new BlockPos[0];
	}

	public static final boolean particleDebug = false;

	public default Vec3 getDebugParticlePosMK2() {
		TileEntity te = (TileEntity) this;
		Vec3 vec = Vec3.createVectorHelper(te.xCoord + 0.5, te.yCoord + 1, te.zCoord + 0.5);
		return vec;
	}

	public default void provideInfoForECMK2(NBTTagCompound data) {
		data.setLong(CompatEnergyControl.L_ENERGY_HE, this.getPower());
		data.setLong(CompatEnergyControl.L_CAPACITY_HE, this.getMaxPower());
	}
}
