package api.hbm.energymk2;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm.tileentity.network.TileEntityPylonBase;
import com.hbm.tileentity.network.TileEntityVoltageCable;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.uninos.NodeNet;
import com.hbm.util.Tuple.Pair;

import java.util.Map.Entry;

import api.hbm.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import api.hbm.energymk2.Nodespace.PowerNode;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Technically MK3 since it's now UNINOS compatible, although UNINOS was build out of 95% nodespace code
 *
 * @author hbm
 */
public class PowerNetMK2 extends NodeNet<IEnergyReceiverMK2, IEnergyProviderMK2, PowerNode> {


	private void punishLegacyVoltageMismatches(List<Pair<IEnergyProviderMK2, Long>> providers, List<Pair<IEnergyReceiverMK2, Long>>[] receivers, long[] demand) {

		if(VoltageEnforcement.isLegacy()) return; // legacy mode: mixed-tier networks keep working exactly as before

		Set<Long> providerVoltages = new HashSet<Long>();
		for(Pair<IEnergyProviderMK2, Long> p : providers) {
			long v = p.getKey().getProviderVoltage();
			if(VoltageTier.isConfigured(v)) providerVoltages.add(v);
		}
		if(providerVoltages.isEmpty()) return; // nothing configured providing here, nothing to compare against

		boolean strict = VoltageEnforcement.isStrict();

		for(int i = 0; i < receivers.length; i++) {
			Iterator<Pair<IEnergyReceiverMK2, Long>> it = receivers[i].iterator();
			while(it.hasNext()) {
				Pair<IEnergyReceiverMK2, Long> entry = it.next();
				long receiverVoltage = entry.getKey().getReceiverVoltage();
				if(!VoltageTier.isConfigured(receiverVoltage)) continue;

				for(Long providerVoltage : providerVoltages) {
					if(providerVoltage.longValue() != receiverVoltage) {
						entry.getKey().onOvervoltage(providerVoltage.longValue());
						// in warn mode the mismatch is only reported, power must keep flowing
						if(strict) {
							demand[i] -= entry.getValue();
							it.remove();
						}
						break;
					}
				}
			}
		}
	}
	public long energyTracker = 0L;

	private final Map<IEnergyProviderMK2, List<DirPos>> providerCablePorts = new HashMap<IEnergyProviderMK2, List<DirPos>>();
	private final Map<IEnergyReceiverMK2, List<DirPos>> receiverCablePorts = new HashMap<IEnergyReceiverMK2, List<DirPos>>();

	protected static int timeout = 3_000;

	@Override public void resetTrackers() { this.energyTracker = 0; }

	public void addProviderAt(IEnergyProviderMK2 provider, int x, int y, int z, ForgeDirection cableFace) {
		super.addProvider(provider);
		rememberCablePort(providerCablePorts, provider, x, y, z, cableFace);
	}

	public void addReceiverAt(IEnergyReceiverMK2 receiver, int x, int y, int z, ForgeDirection cableFace) {
		super.addReceiver(receiver);
		rememberCablePort(receiverCablePorts, receiver, x, y, z, cableFace);
	}

	private <T> void rememberCablePort(Map<T, List<DirPos>> ports, T endpoint, int x, int y, int z, ForgeDirection cableFace) {
		if(cableFace == null || cableFace == ForgeDirection.UNKNOWN) return;
		List<DirPos> list = ports.get(endpoint);
		if(list == null) {
			list = new ArrayList<DirPos>();
			ports.put(endpoint, list);
		}
		for(DirPos port : list) {
			if(port.getX() == x && port.getY() == y && port.getZ() == z && port.getDir() == cableFace) return;
		}
		list.add(new DirPos(x, y, z, cableFace));
	}

	@Override
	public void joinNetworks(NodeNet network) {
		if(network != this && network instanceof PowerNetMK2) {
			PowerNetMK2 other = (PowerNetMK2) network;
			mergeCablePorts(providerCablePorts, other.providerCablePorts);
			mergeCablePorts(receiverCablePorts, other.receiverCablePorts);
		}
		super.joinNetworks(network);
	}

	private <T> void mergeCablePorts(Map<T, List<DirPos>> destination, Map<T, List<DirPos>> source) {
		for(Entry<T, List<DirPos>> entry : source.entrySet()) {
			for(DirPos port : entry.getValue()) {
				rememberCablePort(destination, entry.getKey(), port.getX(), port.getY(), port.getZ(), port.getDir());
			}
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		providerCablePorts.clear();
		receiverCablePorts.clear();
	}

	@Override
	public void update() {

		if(providerEntries.isEmpty()) return;
		if(receiverEntries.isEmpty()) return;

		World world = getNetworkWorld();
		if(world != null && hasVoltageCable(world)) {
			updateVoltageCableNetwork(world);
			return;
		}

		long timestamp = System.currentTimeMillis();

		List<Pair<IEnergyProviderMK2, Long>> providers = new ArrayList();
		long powerAvailable = 0;

		// sum up available power
		Iterator<Entry<IEnergyProviderMK2, Long>> provIt = providerEntries.entrySet().iterator();
		while(provIt.hasNext()) {
			Entry<IEnergyProviderMK2, Long> entry = provIt.next();
			if(timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { providerCablePorts.remove(entry.getKey()); provIt.remove(); continue; }
			long src = Math.min(entry.getKey().getProviderPower(), entry.getKey().getProviderSpeed());
			if(src > 0) {
				providers.add(new Pair(entry.getKey(), src));
				powerAvailable += src;
			}
		}

		// sum up total demand, categorized by priority
		List<Pair<IEnergyReceiverMK2, Long>>[] receivers = new ArrayList[ConnectionPriority.values().length];
		for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList();
		long[] demand = new long[ConnectionPriority.values().length];
		long totalDemand = 0;

		Iterator<Entry<IEnergyReceiverMK2, Long>> recIt = receiverEntries.entrySet().iterator();

		while(recIt.hasNext()) {
			Entry<IEnergyReceiverMK2, Long> entry = recIt.next();
			if(timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { receiverCablePorts.remove(entry.getKey()); recIt.remove(); continue; }
			long rec = Math.min(entry.getKey().getReceiverMaxPower() - entry.getKey().getReceiverPower(), entry.getKey().getReceiverSpeed());
			if(rec > 0) {
				int p = entry.getKey().getPriority().ordinal();
				receivers[p].add(new Pair(entry.getKey(), rec));
				demand[p] += rec;
				totalDemand += rec;
			}
		}

		punishLegacyVoltageMismatches(providers, receivers, demand);
		totalDemand = 0;
		for(long d : demand) totalDemand += d;

		long toTransfer = Math.min(powerAvailable, totalDemand);
		long energyUsed = 0;

		// add power to receivers, ordered by priority
		for(int i = ConnectionPriority.values().length - 1; i >= 0; i--) {
			List<Pair<IEnergyReceiverMK2, Long>> list = receivers[i];
			long priorityDemand = demand[i];

			for(Pair<IEnergyReceiverMK2, Long> entry : list) {
				double weight = (double) entry.getValue() / (double) (priorityDemand);
				long toSend = (long) Math.min(Math.max(toTransfer * weight, 0D), entry.getValue());
				energyUsed += (toSend - entry.getKey().transferPower(toSend)); //leftovers are subtracted from the intended amount to use up
			}

			toTransfer -= energyUsed;
		}

		this.energyTracker += energyUsed;
		long leftover = energyUsed;

		// remove power from providers
		for(Pair<IEnergyProviderMK2, Long> entry : providers) {
			double weight = (double) entry.getValue() / (double) powerAvailable;
			long toUse = (long) Math.max(energyUsed * weight, 0D);
			entry.getKey().usePower(toUse);
			leftover -= toUse;
		}

		// rounding error compensation, detects surplus that hasn't been used and removes it from random providers
		int iterationsLeft = 100; // whiles without emergency brakes are a bad idea
		while(iterationsLeft > 0 && leftover > 0 && providers.size() > 0) {
			iterationsLeft--;

			Pair<IEnergyProviderMK2, Long> selected = providers.get(rand.nextInt(providers.size()));
			IEnergyProviderMK2 scapegoat = selected.getKey();

			long toUse = Math.min(leftover, scapegoat.getProviderPower());
			scapegoat.usePower(toUse);
			leftover -= toUse;
		}
	}

	public long sendPowerDiode(long power) {

		if(receiverEntries.isEmpty()) return power;

		long timestamp = System.currentTimeMillis();

		List<Pair<IEnergyReceiverMK2, Long>>[] receivers = new ArrayList[ConnectionPriority.values().length];
		for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList();
		long[] demand = new long[ConnectionPriority.values().length];
		long totalDemand = 0;

		Iterator<Entry<IEnergyReceiverMK2, Long>> recIt = receiverEntries.entrySet().iterator();

		while(recIt.hasNext()) {
			Entry<IEnergyReceiverMK2, Long> entry = recIt.next();
			if(timestamp - entry.getValue() > timeout) { receiverCablePorts.remove(entry.getKey()); recIt.remove(); continue; }
			long rec = Math.min(entry.getKey().getReceiverMaxPower() - entry.getKey().getReceiverPower(), entry.getKey().getReceiverSpeed());
			int p = entry.getKey().getPriority().ordinal();
			receivers[p].add(new Pair(entry.getKey(), rec));
			demand[p] += rec;
			totalDemand += rec;
		}

		long toTransfer = Math.min(power, totalDemand);
		long energyUsed = 0;

		for(int i = ConnectionPriority.values().length - 1; i >= 0; i--) {
			List<Pair<IEnergyReceiverMK2, Long>> list = receivers[i];
			long priorityDemand = demand[i];

			for(Pair<IEnergyReceiverMK2, Long> entry : list) {
				double weight = (double) entry.getValue() / (double) (priorityDemand);
				long toSend = (long) Math.max(toTransfer * weight, 0D);
				energyUsed += (toSend - entry.getKey().transferPower(toSend)); //leftovers are subtracted from the intended amount to use up
			}

			toTransfer -= energyUsed;
		}

		this.energyTracker += energyUsed;

		return power - energyUsed;
	}

	private void updateVoltageCableNetwork(World world) {
		long timestamp = System.currentTimeMillis();
		List<IEnergyProviderMK2> providers = new ArrayList<IEnergyProviderMK2>();
		Iterator<Entry<IEnergyProviderMK2, Long>> provIt = providerEntries.entrySet().iterator();
		while(provIt.hasNext()) {
			Entry<IEnergyProviderMK2, Long> entry = provIt.next();
			if(timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { providerCablePorts.remove(entry.getKey()); provIt.remove(); continue; }
			providers.add(entry.getKey());
		}

		for(PowerNode node : this.links) {
			for(BlockPos pos : node.positions) {
				TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
				if(tile instanceof IVoltageCableMK2) ((IVoltageCableMK2) tile).beginPowerTick();
			}
		}

		@SuppressWarnings("unchecked")
		List<IEnergyReceiverMK2>[] receivers = new ArrayList[ConnectionPriority.values().length];
		for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList<IEnergyReceiverMK2>();
		Iterator<Entry<IEnergyReceiverMK2, Long>> recIt = receiverEntries.entrySet().iterator();
		while(recIt.hasNext()) {
			Entry<IEnergyReceiverMK2, Long> entry = recIt.next();
			if(timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { receiverCablePorts.remove(entry.getKey()); recIt.remove(); continue; }
			receivers[entry.getKey().getPriority().ordinal()].add(entry.getKey());
		}

		for(int priority = receivers.length - 1; priority >= 0; priority--) {
			for(IEnergyReceiverMK2 receiver : receivers[priority]) {
				for(IEnergyProviderMK2 provider : providers) {
					if((Object) provider == (Object) receiver) continue;
					if(isBadLink(provider) || isBadLink(receiver)) continue;
					VoltageRoute route = findVoltageRoute(world, provider, receiver);
					if(route == null || route.cables.isEmpty()) continue;
					moveAlongRoute(provider, receiver, route);
					if(getReceiverDemand(receiver) <= 0) break;
				}
			}
		}
	}

	private long getReceiverDemand(IEnergyReceiverMK2 receiver) {
		return Math.max(0L, Math.min(receiver.getReceiverMaxPower() - receiver.getReceiverPower(), receiver.getReceiverSpeed()));
	}

	private void moveAlongRoute(IEnergyProviderMK2 provider, IEnergyReceiverMK2 receiver, VoltageRoute route) {
		long voltage = route.cables.get(0).getCableProperties().voltage;
		long sourceAvailable = Math.max(0L, Math.min(provider.getProviderPower(), provider.getProviderSpeed()));
		boolean deny = VoltageEnforcement.shouldDenyTransfer();
		for(IVoltageCableMK2 cable : route.cables) {
			if(cable.getCableProperties().voltage != voltage) {
				if(sourceAvailable > 0) cable.explodeForWrongVoltage(voltage);
				if(deny) return; // legacy/warn mode: only report, power keeps flowing
			}
		}
		long providerVoltage = provider.getProviderVoltage();
		if(VoltageTier.isConfigured(providerVoltage) && providerVoltage != voltage && sourceAvailable > 0) {
			route.cables.get(0).explodeForWrongVoltage(providerVoltage);
			if(deny) return; // legacy/warn mode: mismatched legacy source conducts like before
		}
		long demand = getReceiverDemand(receiver);
		if(sourceAvailable <= 0 || demand <= 0) return;

		long lossBeforeCable = 0L;
		long totalLoss = 0L;
		long maximumPacket = sourceAvailable;
		for(int i = 0; i < route.cables.size(); i++) {
			IVoltageCableMK2 cable = route.cables.get(i);
			maximumPacket = Math.min(maximumPacket, addClamped(cable.getRemainingTransfer(), lossBeforeCable));
			totalLoss = addClamped(totalLoss, getRouteStepLoss(route.cables, i));
			lossBeforeCable = totalLoss;
		}

		long packet = Math.min(maximumPacket, addClamped(demand, totalLoss));
		if(packet <= 0) return;

		long throughCable = packet;
		for(int i = 0; i < route.cables.size(); i++) {
			IVoltageCableMK2 cable = route.cables.get(i);
			throughCable = cable.useTransferCapacity(throughCable);
			throughCable = Math.max(0L, throughCable - getRouteStepLoss(route.cables, i));
		}

		provider.usePower(packet);
		if(throughCable > 0) {
			long remainder = receiver.transferPowerAtVoltage(throughCable, voltage);
			energyTracker += throughCable - remainder;
		}
	}

	private long addClamped(long first, long second) {
		return first > Long.MAX_VALUE - second ? Long.MAX_VALUE : first + second;
	}

	/** Loss paid to get past the element at index i. Regular cables lose their per-block loss, pylon spans lose less per block the farther apart the pylons are. */
	private long getRouteStepLoss(List<IVoltageCableMK2> cables, int i) {
		IVoltageCableMK2 cable = cables.get(i);
		if(cable instanceof TileEntityPylonBase) {
			if(i + 1 >= cables.size()) return 0L;
			IVoltageCableMK2 next = cables.get(i + 1);
			if(!(next instanceof TileEntityPylonBase)) return 0L;
			return ((TileEntityPylonBase) cable).getSpanLoss(next);
		}
		return cable.getCableProperties().lossPerBlock;
	}

	private World getNetworkWorld() {
		for(IEnergyProviderMK2 provider : providerEntries.keySet()) {
			if(provider instanceof TileEntity && ((TileEntity) provider).getWorldObj() != null) return ((TileEntity) provider).getWorldObj();
		}
		for(IEnergyReceiverMK2 receiver : receiverEntries.keySet()) {
			if(receiver instanceof TileEntity && ((TileEntity) receiver).getWorldObj() != null) return ((TileEntity) receiver).getWorldObj();
		}
		return null;
	}

	private boolean hasVoltageCable(World world) {
		for(PowerNode node : this.links) {
			for(BlockPos pos : node.positions) {
				if(world.getTileEntity(pos.getX(), pos.getY(), pos.getZ()) instanceof TileEntityVoltageCable) return true;
			}
		}
		return false;
	}

	private VoltageRoute findVoltageRoute(World world, IEnergyProviderMK2 provider, IEnergyReceiverMK2 receiver) {
		Map<BlockPos, PowerNode> nodes = new HashMap<BlockPos, PowerNode>();
		Set<BlockPos> targets = new HashSet<BlockPos>();
		ArrayDeque<PowerNode> queue = new ArrayDeque<PowerNode>();
		Map<PowerNode, PowerNode> previous = new HashMap<PowerNode, PowerNode>();

		for(PowerNode node : this.links) {
			for(BlockPos pos : node.positions) {
				BlockPos key = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
				nodes.put(key, node);
				TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
				if(tile instanceof IVoltageCableMK2 && touchesEnabledCableFace(world, pos, providerCablePorts.get(provider), provider) && !previous.containsKey(node)) {
					queue.add(node);
					previous.put(node, null);
				}
				if(tile instanceof IVoltageCableMK2 && touchesEnabledCableFace(world, pos, receiverCablePorts.get(receiver), receiver)) targets.add(key);
			}
		}

		PowerNode end = null;
		while(!queue.isEmpty()) {
			PowerNode node = queue.remove();
			if(containsPosition(node, targets)) { end = node; break; }
			for(DirPos connection : node.connections) {
				PowerNode next = nodes.get(new BlockPos(connection.getX(), connection.getY(), connection.getZ()));
				if(next != null && !previous.containsKey(next) && isSameVoltageCableStep(world, node, next)) {
					previous.put(next, node);
					queue.add(next);
				}
			}
		}

		if(end == null) return null;
		List<PowerNode> nodePath = new ArrayList<PowerNode>();
		for(PowerNode node = end; node != null; node = previous.get(node)) nodePath.add(0, node);
		VoltageRoute route = new VoltageRoute();
		for(PowerNode node : nodePath) {
			for(BlockPos pos : node.positions) {
				TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
				if(tile instanceof IVoltageCableMK2) route.cables.add((IVoltageCableMK2) tile);
			}
		}
		return route;
	}

	private boolean isSameVoltageCableStep(World world, PowerNode from, PowerNode to) {
		IVoltageCableMK2 first = getVoltageCable(world, from);
		IVoltageCableMK2 second = getVoltageCable(world, to);
		return first != null && second != null
				&& first.getCableProperties().voltage == second.getCableProperties().voltage;
	}

	private IVoltageCableMK2 getVoltageCable(World world, PowerNode node) {
		for(BlockPos pos : node.positions) {
			TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
			if(tile instanceof IVoltageCableMK2) return (IVoltageCableMK2) tile;
		}
		return null;
	}

	private boolean containsPosition(PowerNode node, Set<BlockPos> positions) {
		for(BlockPos pos : node.positions) if(positions.contains(pos)) return true;
		return false;
	}

	private boolean isAdjacent(BlockPos first, BlockPos second) {
		return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY()) + Math.abs(first.getZ() - second.getZ()) == 1;
	}

	private boolean touchesEnabledCableFace(World world, BlockPos cablePosition, List<DirPos> subscribedPorts, IEnergyHandlerMK2 endpoint) {
		TileEntity tile = world.getTileEntity(cablePosition.getX(), cablePosition.getY(), cablePosition.getZ());
		if(tile instanceof TileEntityProxyBase) {
			TileEntity core = ((TileEntityProxyBase) tile).getTE();
			if(core instanceof TileEntityPylonBase) tile = core;
		}
		if(!(tile instanceof IVoltageCableMK2)) return false;
		IVoltageCableMK2 cable = (IVoltageCableMK2) tile;

		if(subscribedPorts != null && !subscribedPorts.isEmpty()) {
			for(DirPos port : subscribedPorts) {
				if(port.getX() == cablePosition.getX() && port.getY() == cablePosition.getY() && port.getZ() == cablePosition.getZ()) {
					return cable.isFaceConnected(port.getDir());
				}
			}
			return false;
		}

		BlockPos[] connectionPoints = endpoint.getVoltageConnectionPoints();
		if(connectionPoints == null) return false;
		for(BlockPos endpointPosition : connectionPoints) {
			if(endpointPosition == null || !isAdjacent(cablePosition, endpointPosition)) continue;
			for(net.minecraftforge.common.util.ForgeDirection direction : net.minecraftforge.common.util.ForgeDirection.VALID_DIRECTIONS) {
				if(cablePosition.getX() + direction.offsetX == endpointPosition.getX()
						&& cablePosition.getY() + direction.offsetY == endpointPosition.getY()
						&& cablePosition.getZ() + direction.offsetZ == endpointPosition.getZ()) {
					return cable.isFaceConnected(direction);
				}
			}
		}
		return false;
	}

	private static class VoltageRoute {
		final List<IVoltageCableMK2> cables = new ArrayList<IVoltageCableMK2>();
	}
}
