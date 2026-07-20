package api.hbm.energymk3;

import api.hbm.tile.ILoadedTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

public interface IEnergyHandlerMK3 extends IEnergyConnectorMK3, ILoadedTile {

	public long getPower();
	public void setPower(long power);
	public long getMaxPower();

	public long getVoltageNominal();
	public double getVoltageTolerance();
	public double getEfficiency();

	public static final boolean particleDebug = false;

	public default Vec3 getDebugParticlePosMK3() {
		TileEntity te = (TileEntity) this;
		return Vec3.createVectorHelper(te.xCoord + 0.5, te.yCoord + 1, te.zCoord + 0.5);
	}
}
