package api.hbm.energymk2;

public final class CableProperties {

	public final String id;
	public final long voltage;
	public final long maxEnergyPerTick;
	public final long lossPerBlock;

	public CableProperties(String id, long voltage, long maxEnergyPerTick, long lossPerBlock) {
		if(id == null || id.isEmpty()) throw new IllegalArgumentException("Cable id cannot be empty");
		if(voltage <= 0 || maxEnergyPerTick <= 0 || lossPerBlock < 0) {
			throw new IllegalArgumentException("Cable properties must be positive; loss may be zero");
		}
		this.id = id;
		this.voltage = voltage;
		this.maxEnergyPerTick = maxEnergyPerTick;
		this.lossPerBlock = lossPerBlock;
	}
}
