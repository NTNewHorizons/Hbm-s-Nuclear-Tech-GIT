package com.hbm.uninos.networkproviders;

import com.hbm.uninos.INetworkProvider;

import api.hbm.energymk3.PowerNetMK3;

public class PowerNetProviderMK3 implements INetworkProvider<PowerNetMK3> {

	@Override
	public PowerNetMK3 provideNetwork() {
		return new PowerNetMK3();
	}
}
