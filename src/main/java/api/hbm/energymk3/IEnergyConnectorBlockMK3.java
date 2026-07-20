package api.hbm.energymk3;

import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

public interface IEnergyConnectorBlockMK3 {

	public boolean canConnect(IBlockAccess world, int x, int y, int z, ForgeDirection dir);
}
