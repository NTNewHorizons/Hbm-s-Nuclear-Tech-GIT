package api.hbm.energymk3;

import com.hbm.lib.Library;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;
import net.minecraft.tileentity.TileEntity;

public interface IEnergyConductorMK3 extends IEnergyConnectorMK3 {

	public default PowerNodeMK3 createNode() {
		TileEntity tile = (TileEntity) this;
		return new PowerNodeMK3(new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord)).setConnections(
				new DirPos(tile.xCoord + 1, tile.yCoord, tile.zCoord, Library.POS_X),
				new DirPos(tile.xCoord - 1, tile.yCoord, tile.zCoord, Library.NEG_X),
				new DirPos(tile.xCoord, tile.yCoord + 1, tile.zCoord, Library.POS_Y),
				new DirPos(tile.xCoord, tile.yCoord - 1, tile.zCoord, Library.NEG_Y),
				new DirPos(tile.xCoord, tile.yCoord, tile.zCoord + 1, Library.POS_Z),
				new DirPos(tile.xCoord, tile.yCoord, tile.zCoord - 1, Library.NEG_Z)
				);
	}

	public double getResistance();
	public double getMaxAmperage();
	public long getMaxThroughput();
	public long getInternalBuffer();
}
