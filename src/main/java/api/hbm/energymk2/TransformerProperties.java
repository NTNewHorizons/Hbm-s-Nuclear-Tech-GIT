package api.hbm.energymk2;

public final class TransformerProperties {

	public final String id;
	public final long inputVoltage;
	public final long outputVoltage;
	public final long maxInputEnergyPerTick;

	public TransformerProperties(String id, long inputVoltage, long outputVoltage, long maxInputEnergyPerTick) {
		if(id == null || id.isEmpty()) throw new IllegalArgumentException("Transformer id cannot be empty");
		if(inputVoltage <= 0 || outputVoltage <= 0 || maxInputEnergyPerTick <= 0) {
			throw new IllegalArgumentException("Transformer properties must be positive");
		}
		this.id = id;
		this.inputVoltage = inputVoltage;
		this.outputVoltage = outputVoltage;
		this.maxInputEnergyPerTick = maxInputEnergyPerTick;
	}

	public long convert(long inputEnergy) {
		if(inputEnergy <= 0) return 0;
		if(inputEnergy > Long.MAX_VALUE / inputVoltage) return Long.MAX_VALUE / outputVoltage;
		return inputEnergy * inputVoltage / outputVoltage;
	}

	public long getMaxOutputEnergyPerTick() {
		return convert(maxInputEnergyPerTick);
	}
}
