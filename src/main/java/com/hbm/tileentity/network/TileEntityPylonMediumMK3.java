package com.hbm.tileentity.network;

import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPylonMediumMK3 extends TileEntityPylonBaseMK3 {

	protected boolean hasTransformer = false;

	@Override
	public ConnectionType getConnectionType() { return ConnectionType.TRIPLE; }

	@Override
	public Vec3[] getMountPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		double height = 7.5D;
		return new Vec3[] {
				Vec3.createVectorHelper(0.5, height, 0.5),
				Vec3.createVectorHelper(0.5 + dir.offsetX, height, 0.5 + dir.offsetZ),
				Vec3.createVectorHelper(0.5 + dir.offsetX * 2, height, 0.5 + dir.offsetZ * 2),
		};
	}

	@Override
	public double getMaxWireLength() { return 45; }

	@Override
	public VoltageTier getMaxVoltageTier() { return VoltageTier.HV; }

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return this.hasTransformer ? ForgeDirection.getOrientation(this.getBlockMetadata() - 10).getOpposite() == dir : false;
	}

	public void setHasTransformer(boolean has) { this.hasTransformer = has; }

	@Override
	public PowerNodeMK3 createNode() {
		TileEntity tile = (TileEntity) this;
		PowerNodeMK3 node = new PowerNodeMK3(new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord))
				.setCableProps(this.voltageTier, this.resistance, this.maxAmperage, this.maxPower, this.internalBuffer)
				.setConnections(new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN));
		for(int[] pos : this.connected) node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));
		if(this.hasTransformer) {
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10).getOpposite();
			node.addConnection(new DirPos(xCoord + dir.offsetX, yCoord, zCoord + dir.offsetZ, dir));
		}
		return node;
	}
}
