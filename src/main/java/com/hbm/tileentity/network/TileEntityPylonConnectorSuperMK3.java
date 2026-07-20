package com.hbm.tileentity.network;

import api.hbm.energymk3.VoltageTier;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPylonConnectorSuperMK3 extends TileEntityPylonConnectorMK3 {

	@Override
	public Vec3[] getMountPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata());
		return new Vec3[] {Vec3.createVectorHelper(0.5 + dir.offsetX * 0.375, 0.5 + dir.offsetY * 0.375, 0.5 + dir.offsetZ * 0.375)};
	}

	@Override
	public double getMaxWireLength() { return 100; }

	@Override
	public VoltageTier getMaxVoltageTier() { return VoltageTier.MV; }
}
