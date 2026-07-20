package api.hbm.energymk3;

import com.hbm.uninos.GenNode;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PowerNetProviderMK3;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import net.minecraft.world.World;

public class NodespaceMK3 {

	public static final PowerNetProviderMK3 THE_POWER_PROVIDER_MK3 = new PowerNetProviderMK3();

	public static PowerNodeMK3 getNode(World world, int x, int y, int z) {
		return (PowerNodeMK3) UniNodespace.getNode(world, x, y, z, THE_POWER_PROVIDER_MK3);
	}

	public static void createNode(World world, PowerNodeMK3 node) {
		UniNodespace.createNode(world, node);
	}

	public static void destroyNode(World world, int x, int y, int z) {
		UniNodespace.destroyNode(world, x, y, z, THE_POWER_PROVIDER_MK3);
	}

	public static class PowerNodeMK3 extends GenNode<PowerNetMK3> {

		public VoltageTier voltageTier;
		public double resistance;
		public double maxAmperage;
		public long maxPower;
		public long internalBuffer;

		public PowerNodeMK3(BlockPos... positions) {
			super(THE_POWER_PROVIDER_MK3, positions);
			this.positions = positions;
			this.voltageTier = VoltageTier.LV;
			this.resistance = 0;
			this.maxAmperage = 0;
			this.maxPower = 0;
			this.internalBuffer = 0;
		}

		public PowerNodeMK3 setCableProps(VoltageTier tier, double resistance, double maxAmperage, long maxPower, long internalBuffer) {
			this.voltageTier = tier;
			this.resistance = resistance;
			this.maxAmperage = maxAmperage;
			this.maxPower = maxPower;
			this.internalBuffer = internalBuffer;
			return this;
		}

		@Override
		public PowerNodeMK3 setConnections(DirPos... connections) {
			super.setConnections(connections);
			return this;
		}
	}
}
