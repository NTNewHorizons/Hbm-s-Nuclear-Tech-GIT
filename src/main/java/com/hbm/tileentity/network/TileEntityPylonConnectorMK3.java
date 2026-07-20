package com.hbm.tileentity.network;

import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPylonConnectorMK3 extends TileEntityPylonBaseMK3 {

	@Override
	public ConnectionType getConnectionType() { return ConnectionType.SINGLE; }

	@Override
	public Vec3[] getMountPos() {
		return new Vec3[] {Vec3.createVectorHelper(0.5, 0.5, 0.5)};
	}

	@Override
	public double getMaxWireLength() { return 10; }

	@Override
	public PowerNodeMK3 createNode() {
		TileEntity tile = (TileEntity) this;
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata()).getOpposite();
		PowerNodeMK3 node = new PowerNodeMK3(new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord))
				.setCableProps(this.voltageTier, this.resistance, this.maxAmperage, this.maxPower, this.internalBuffer)
				.setConnections(
					new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN),
					new DirPos(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir));
		for(int[] pos : this.connected) node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));
		return node;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return ForgeDirection.getOrientation(this.getBlockMetadata()).getOpposite() == dir;
	}

	@Override
	public VoltageTier getMaxVoltageTier() { return VoltageTier.LV; }
}
