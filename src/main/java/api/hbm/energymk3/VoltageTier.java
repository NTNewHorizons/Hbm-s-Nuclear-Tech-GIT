package api.hbm.energymk3;

public enum VoltageTier {
	LV(120),
	MV(480),
	HV(1920),
	EV(7680),
	SC(30720),
	BUS(Long.MAX_VALUE);

	private long voltage;

	private VoltageTier(long voltage) {
		this.voltage = voltage;
	}

	public long getVoltage() {
		return this.voltage;
	}

	public static VoltageTier fromVoltage(long voltage) {
		if(voltage >= BUS.voltage) return BUS;
		if(voltage >= SC.voltage) return SC;
		if(voltage >= EV.voltage) return EV;
		if(voltage >= HV.voltage) return HV;
		if(voltage >= MV.voltage) return MV;
		return LV;
	}
}
