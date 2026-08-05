package api.hbm.energymk2;

import net.minecraftforge.common.util.ForgeDirection;

public interface IVoltageCableMK2 extends IEnergyConductorMK2 {

	public CableProperties getCableProperties();
	public void beginPowerTick();
	public long getRemainingTransfer();
	public long useTransferCapacity(long amount);
	public void explodeForWrongVoltage(long suppliedVoltage);

	public default boolean isFaceConnected(ForgeDirection direction) {
		return direction != null && direction != ForgeDirection.UNKNOWN;
	}
}
