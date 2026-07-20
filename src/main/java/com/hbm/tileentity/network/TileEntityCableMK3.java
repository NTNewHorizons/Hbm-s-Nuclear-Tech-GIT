package com.hbm.tileentity.network;

import api.hbm.energymk3.IEnergyConductorMK3;
import api.hbm.energymk3.NodespaceMK3;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import api.hbm.energymk3.VoltageTier;
import com.hbm.tileentity.TileEntityLoadedBase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityCableMK3 extends TileEntityLoadedBase implements IEnergyConductorMK3 {

	protected PowerNodeMK3 node;

	public VoltageTier voltageTier = VoltageTier.LV;
	public double resistance = 0.01;
	public double maxAmperage = 10;
	public long maxPower = 1000;
	public long internalBuffer = 100;

	public double syncHeat;
	public boolean isOverheating;

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(this.node == null || this.node.expired) {
				if(this.shouldCreateNode()) {
					this.node = NodespaceMK3.getNode(worldObj, xCoord, yCoord, zCoord);
					if(this.node == null || this.node.expired) {
						this.node = this.createNode();
						this.node.setCableProps(this.voltageTier, this.resistance, this.maxAmperage, this.maxPower, this.internalBuffer);
						NodespaceMK3.createNode(worldObj, this.node);
					}
				}
			}

			if(this.node != null && this.node.hasValidNet()) {
				Double heat = this.node.net.cableHeat.get(this.node);
				this.syncHeat = heat != null ? heat : 0.0;
				this.isOverheating = this.syncHeat > 0.5;
			} else {
				this.syncHeat = 0.0;
				this.isOverheating = false;
			}

			this.networkPackNT(25);

			if(this.canOverloadMelt() && this.isOverheating && this.syncHeat > 1.0) {
				worldObj.newExplosion(null, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, 0.5F, false, true);
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			}
		}
	}

	public boolean canOverloadMelt() {
		return true;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeDouble(this.syncHeat);
		buf.writeBoolean(this.isOverheating);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.syncHeat = buf.readDouble();
		this.isOverheating = buf.readBoolean();
	}

	@SideOnly(Side.CLIENT)
	public double getHeatForRender() {
		return this.syncHeat;
	}

	public boolean shouldCreateNode() {
		return true;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if(!worldObj.isRemote) {
			if(this.node != null) {
				NodespaceMK3.destroyNode(worldObj, xCoord, yCoord, zCoord);
			}
		}
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN;
	}

	@Override
	public VoltageTier getVoltageTier() {
		return this.voltageTier;
	}

	@Override
	public double getResistance() {
		return this.resistance;
	}

	@Override
	public double getMaxAmperage() {
		return this.maxAmperage;
	}

	@Override
	public long getMaxThroughput() {
		return this.maxPower;
	}

	@Override
	public long getInternalBuffer() {
		return this.internalBuffer;
	}
}
