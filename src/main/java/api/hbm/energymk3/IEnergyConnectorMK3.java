package api.hbm.energymk3;

import net.minecraftforge.common.util.ForgeDirection;

public interface IEnergyConnectorMK3 {

	public default boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN;
	}

	public VoltageTier getVoltageTier();
}
