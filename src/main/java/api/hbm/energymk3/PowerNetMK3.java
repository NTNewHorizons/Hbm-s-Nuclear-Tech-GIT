package api.hbm.energymk3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hbm.uninos.NodeNet;

import api.hbm.energymk3.IEnergyReceiverMK3.ConnectionPriority;
import api.hbm.energymk3.NodespaceMK3.PowerNodeMK3;

public class PowerNetMK3 extends NodeNet<IEnergyReceiverMK3, IEnergyProviderMK3, PowerNodeMK3> {

	public long energyTracker = 0L;
	public long powerAvailable = 0L;
	public long totalDemand = 0L;
	public long powerTransferred = 0L;

	public Map<PowerNodeMK3, Long> cableLoad = new HashMap();
	public Map<PowerNodeMK3, Double> cableHeat = new HashMap();

	protected static int timeout = 3_000;

	@Override public void resetTrackers() {
		this.energyTracker = 0;
		this.powerAvailable = 0;
		this.totalDemand = 0;
		this.powerTransferred = 0;
		this.cableLoad.clear();
	}

	public VoltageTier getNetworkTier() {
		VoltageTier tier = null;
		for(PowerNodeMK3 link : this.links) {
			VoltageTier linkTier = link.voltageTier;
			if(linkTier == null) continue;
			if(tier == null) {
				tier = linkTier;
			} else if(tier != linkTier) {
				return null;
			}
		}
		return tier;
	}

	public double getNetworkResistance() {
		if(this.links.isEmpty()) return 0;
		double totalR = 0;
		int count = 0;
		for(PowerNodeMK3 link : this.links) {
			totalR += link.resistance;
			count++;
		}
		return totalR / Math.max(1, count);
	}

	public long getMinMaxPower() {
		long min = Long.MAX_VALUE;
		for(PowerNodeMK3 link : this.links) {
			if(link.maxPower > 0 && link.maxPower < min) {
				min = link.maxPower;
			}
		}
		return min == Long.MAX_VALUE ? Long.MAX_VALUE : min;
	}

	@Override
	public void update() {

		if(providerEntries.isEmpty()) return;
		if(receiverEntries.isEmpty()) return;

		VoltageTier tier = this.getNetworkTier();
		if(tier == null) return;
		long vNominal = tier.getVoltage();

		long timestamp = System.currentTimeMillis();

		List<IEnergyProviderMK3> providers = new ArrayList();
		List<Long> providerSrcs = new ArrayList();
		this.powerAvailable = 0;

		Iterator<Entry<IEnergyProviderMK3, Long>> provIt = providerEntries.entrySet().iterator();
		while(provIt.hasNext()) {
			Entry<IEnergyProviderMK3, Long> entry = provIt.next();
			if(timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { provIt.remove(); continue; }
			long src = Math.min(entry.getKey().getPower(), entry.getKey().getProviderSpeed());
			if(src > 0) {
				providers.add(entry.getKey());
				providerSrcs.add(src);
				powerAvailable += src;
			}
		}

		List<IEnergyReceiverMK3>[] receivers = new ArrayList[ConnectionPriority.values().length];
		List<Long>[] receiverDemands = new ArrayList[ConnectionPriority.values().length];
		for(int i = 0; i < receivers.length; i++) {
			receivers[i] = new ArrayList();
			receiverDemands[i] = new ArrayList();
		}
		long[] demand = new long[ConnectionPriority.values().length];
		this.totalDemand = 0;

		Iterator<Entry<IEnergyReceiverMK3, Long>> recIt = receiverEntries.entrySet().iterator();
		while(recIt.hasNext()) {
			Entry<IEnergyReceiverMK3, Long> entry = recIt.next();
			if(timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { recIt.remove(); continue; }
			long rec = Math.min(entry.getKey().getMaxPower() - entry.getKey().getPower(), entry.getKey().getReceiverSpeed());
			if(rec > 0) {
				int p = entry.getKey().getPriority().ordinal();
				receivers[p].add(entry.getKey());
				receiverDemands[p].add(rec);
				demand[p] += rec;
				totalDemand += rec;
			}
		}

		double networkResistance = this.getNetworkResistance();
		long minMaxPower = this.getMinMaxPower();

		long toTransfer = Math.min(powerAvailable, totalDemand);
		long cableLimited = Math.min(toTransfer, minMaxPower);

		double current = (double) cableLimited / (double) vNominal;
		double i2rLoss = current * current * networkResistance;
		long lossRounded = (long) Math.floor(i2rLoss);
		long availableAfterLoss = cableLimited - lossRounded;

		if(availableAfterLoss <= 0) {
			for(PowerNodeMK3 link : this.links) {
				if(link.maxPower > 0) {
					this.cableLoad.put(link, 0L);
					double heat = Math.max(0, this.cableHeat.getOrDefault(link, 0.0) - 0.01);
					this.cableHeat.put(link, heat);
				}
			}
			return;
		}

		long energyUsed = 0;

		for(int i = ConnectionPriority.values().length - 1; i >= 0; i--) {
			List<IEnergyReceiverMK3> list = receivers[i];
			List<Long> dems = receiverDemands[i];
			long priorityDemand = demand[i];

			if(priorityDemand > 0 && !list.isEmpty()) {
				for(int j = 0; j < list.size(); j++) {
					IEnergyReceiverMK3 recv = list.get(j);
					double weight = (double) dems.get(j) / (double) priorityDemand;
					long toSend = (long) Math.max(availableAfterLoss * weight, 0D);
					toSend = Math.min(toSend, dems.get(j));
					energyUsed += (toSend - recv.transferPower(toSend));

					long vRecv = recv.getVoltageNominal();
					double tolerance = recv.getVoltageTolerance();
					long vMin = (long) (vRecv * (1 - tolerance));
					long vMax = (long) (vRecv * (1 + tolerance));

					if(vNominal < vMin) {
						recv.onUndervoltage(vNominal, vRecv);
					} else if(vNominal > vMax) {
						recv.onOvervoltage(vNominal, vRecv);
					}
				}
			}

			availableAfterLoss -= energyUsed;
			if(availableAfterLoss <= 0) break;
		}

		long actuallyTransferred = energyUsed;
		this.energyTracker += actuallyTransferred;
		this.powerTransferred = actuallyTransferred;
		long leftover = actuallyTransferred;

		for(int i = 0; i < providers.size(); i++) {
			IEnergyProviderMK3 prov = providers.get(i);
			double weight = (double) providerSrcs.get(i) / (double) powerAvailable;
			long toUse = (long) Math.max(actuallyTransferred * weight, 0D);
			toUse = Math.min(toUse, prov.getPower());
			prov.usePower(toUse);
			leftover -= toUse;
		}

		int iterationsLeft = 100;
		while(iterationsLeft > 0 && leftover > 0 && providers.size() > 0) {
			iterationsLeft--;
			IEnergyProviderMK3 scapegoat = providers.get(rand.nextInt(providers.size()));
			long toUse = Math.min(leftover, scapegoat.getPower());
			scapegoat.usePower(toUse);
			leftover -= toUse;
		}

		for(PowerNodeMK3 link : this.links) {
			if(link.maxPower > 0) {
				long load = Math.min(powerTransferred, link.maxPower);
				this.cableLoad.put(link, load);

				double heat = this.cableHeat.getOrDefault(link, 0.0);
				if(load >= link.maxPower * 0.8) {
					double excess = (double) (load - link.maxPower * 0.8) / (double) link.maxPower;
					heat += excess;
				} else {
					heat = Math.max(0, heat - 0.01);
				}
				this.cableHeat.put(link, heat);
			}
		}
	}

	public long sendPowerDiode(long power) {

		if(receiverEntries.isEmpty()) return power;

		VoltageTier tier = this.getNetworkTier();
		if(tier == null) return power;
		long vNominal = tier.getVoltage();

		long timestamp = System.currentTimeMillis();

		List<IEnergyReceiverMK3>[] receivers = new ArrayList[ConnectionPriority.values().length];
		List<Long>[] receiverDemands = new ArrayList[ConnectionPriority.values().length];
		for(int i = 0; i < receivers.length; i++) {
			receivers[i] = new ArrayList();
			receiverDemands[i] = new ArrayList();
		}
		long[] demand = new long[ConnectionPriority.values().length];
		long totalDemand = 0;

		Iterator<Entry<IEnergyReceiverMK3, Long>> recIt = receiverEntries.entrySet().iterator();
		while(recIt.hasNext()) {
			Entry<IEnergyReceiverMK3, Long> entry = recIt.next();
			if(timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
			long rec = Math.min(entry.getKey().getMaxPower() - entry.getKey().getPower(), entry.getKey().getReceiverSpeed());
			if(rec > 0) {
				int p = entry.getKey().getPriority().ordinal();
				receivers[p].add(entry.getKey());
				receiverDemands[p].add(rec);
				demand[p] += rec;
				totalDemand += rec;
			}
		}

		double networkResistance = this.getNetworkResistance();
		long minMaxPower = this.getMinMaxPower();

		long toTransfer = Math.min(power, totalDemand);
		long cableLimited = Math.min(toTransfer, minMaxPower);

		double current = (double) cableLimited / (double) vNominal;
		double i2rLoss = current * current * networkResistance;
		long lossRounded = Math.max(1, (long) Math.floor(i2rLoss));
		long availableAfterLoss = cableLimited - lossRounded;

		if(availableAfterLoss <= 0) return power;

		long energyUsed = 0;

		for(int i = ConnectionPriority.values().length - 1; i >= 0; i--) {
			List<IEnergyReceiverMK3> list = receivers[i];
			List<Long> dems = receiverDemands[i];
			long priorityDemand = demand[i];

			if(priorityDemand > 0 && !list.isEmpty()) {
				for(int j = 0; j < list.size(); j++) {
					double weight = (double) dems.get(j) / (double) priorityDemand;
					long toSend = (long) Math.max(availableAfterLoss * weight, 0D);
					toSend = Math.min(toSend, dems.get(j));
					energyUsed += (toSend - list.get(j).transferPower(toSend));
				}
			}

			availableAfterLoss -= energyUsed;
			if(availableAfterLoss <= 0) break;
		}

		this.energyTracker += energyUsed;

		return power - energyUsed;
	}
}
