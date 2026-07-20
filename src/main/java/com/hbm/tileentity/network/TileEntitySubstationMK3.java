package com.hbm.tileentity.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.Library;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntitySubstationMK3 extends TileEntityPylonBaseMK3 {

	@Override
	public ConnectionType getConnectionType() { return ConnectionType.QUAD; }

	@Override
	public Vec3[] getMountPos() {
		double topOff = 5.25;
		Vec3 vec = Vec3.createVectorHelper(1, 0, 0);
		switch(getBlockMetadata() - BlockDummyable.offset) {
		case 2: vec.rotateAroundY((float) Math.PI * 0.0F); break;
		case 4: vec.rotateAroundY((float) Math.PI * 0.5F); break;
		case 3: vec.rotateAroundY((float) Math.PI * 0.0F); break;
		case 5: vec.rotateAroundY((float) Math.PI * 0.5F); break;
		}
		return new Vec3[] {
				Vec3.createVectorHelper(0.5 + vec.xCoord * 0.5, topOff, 0.5 + vec.zCoord * 0.5),
				Vec3.createVectorHelper(0.5 + vec.xCoord * 1.5, topOff, 0.5 + vec.zCoord * 1.5),
				Vec3.createVectorHelper(0.5 - vec.xCoord * 0.5, topOff, 0.5 - vec.zCoord * 0.5),
				Vec3.createVectorHelper(0.5 - vec.xCoord * 1.5, topOff, 0.5 - vec.zCoord * 1.5),
		};
	}

	@Override
	public Vec3 getConnectionPoint() {
		return Vec3.createVectorHelper(xCoord + 0.5, yCoord + 5.25, zCoord + 0.5);
	}

	@Override
	public double getMaxWireLength() { return 20; }

	@Override
	public VoltageTier getMaxVoltageTier() { return VoltageTier.HV; }

	@Override
	public PowerNodeMK3 createNode() {
		TileEntity tile = (TileEntity) this;
		PowerNodeMK3 node = new PowerNodeMK3(
				new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord),
				new BlockPos(tile.xCoord + 1, tile.yCoord, tile.zCoord + 1),
				new BlockPos(tile.xCoord + 1, tile.yCoord, tile.zCoord - 1),
				new BlockPos(tile.xCoord - 1, tile.yCoord, tile.zCoord + 1),
				new BlockPos(tile.xCoord - 1, tile.yCoord, tile.zCoord - 1))
				.setCableProps(this.voltageTier, this.resistance, this.maxAmperage, this.maxPower, this.internalBuffer)
				.setConnections(
					new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN),
					new DirPos(xCoord + 2, yCoord, zCoord - 1, Library.POS_X),
					new DirPos(xCoord + 2, yCoord, zCoord + 1, Library.POS_X),
					new DirPos(xCoord - 2, yCoord, zCoord - 1, Library.NEG_X),
					new DirPos(xCoord - 2, yCoord, zCoord + 1, Library.NEG_X),
					new DirPos(xCoord - 1, yCoord, zCoord + 2, Library.POS_Z),
					new DirPos(xCoord + 1, yCoord, zCoord + 2, Library.POS_Z),
					new DirPos(xCoord - 1, yCoord, zCoord - 2, Library.NEG_Z),
					new DirPos(xCoord + 1, yCoord, zCoord - 2, Library.NEG_Z));
		for(int[] pos : this.connected) node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));
		return node;
	}
}
