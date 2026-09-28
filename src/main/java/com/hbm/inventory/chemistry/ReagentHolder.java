package com.hbm.inventory.chemistry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** A persistent mixture of reagents, measured in SS13-style units. */
public class ReagentHolder {

	public static class Entry {
		public final ChemReagent reagent;
		public double amount;
		public double purity;

		private Entry(ChemReagent reagent, double amount, double purity) {
			this.reagent = reagent;
			this.amount = amount;
			this.purity = purity;
		}
	}

	private final double capacity;
	private final Map<String, Entry> contents = new LinkedHashMap<String, Entry>();
	private double temperature = 293.15D;
	private double ph = 7.0D;

	public ReagentHolder(double capacity) {
		this.capacity = capacity;
	}

	public double add(ChemReagent reagent, double amount) {
		return add(reagent, amount, 1.0D, 293.15D, true);
	}

	public double add(ChemReagent reagent, double amount, double purity, double incomingTemperature, boolean affectHolderPH) {
		if(reagent == null || amount <= 0) return 0;
		double accepted = Math.min(amount, capacity - getTotalAmount());
		if(accepted <= 0) return 0;

		double oldTotal = getTotalAmount();
		Entry entry = contents.get(reagent.id);
		if(entry == null) {
			entry = new Entry(reagent, accepted, clamp01(purity));
			contents.put(reagent.id, entry);
		} else {
			entry.purity = ((entry.purity * entry.amount) + (clamp01(purity) * accepted)) / (entry.amount + accepted);
			entry.amount += accepted;
		}

		temperature = oldTotal <= 0 ? incomingTemperature : ((temperature * oldTotal) + (incomingTemperature * accepted)) / (oldTotal + accepted);
		if(affectHolderPH) ph = oldTotal <= 0 ? reagent.ph : ((ph * oldTotal) + (reagent.ph * accepted)) / (oldTotal + accepted);
		return accepted;
	}

	/** Adds reaction output while retaining the holder's current pH. */
	public double addReactionProduct(ChemReagent reagent, double amount, double purity) {
		ChemReagent result = reagent;
		double resultPurity = clamp01(purity);
		if(reagent.getInverse() != null && resultPurity < reagent.getInversePurity()) {
			result = reagent.getInverse();
			resultPurity = clamp01(1.0D - resultPurity);
		}
		return add(result, amount, resultPurity, temperature, false);
	}

	public double remove(ChemReagent reagent, double amount) {
		Entry entry = contents.get(reagent.id);
		if(entry == null || amount <= 0) return 0;
		double removed = Math.min(amount, entry.amount);
		entry.amount -= removed;
		if(entry.amount < 0.0001D) contents.remove(reagent.id);
		if(contents.isEmpty()) {
			ph = 7.0D;
			temperature = 293.15D;
		}
		return removed;
	}

	public void clear() {
		contents.clear();
		temperature = 293.15D;
		ph = 7.0D;
	}

	public boolean has(ChemReagent reagent, double amount) {
		return getAmount(reagent) + 0.0001D >= amount;
	}

	public double getAmount(ChemReagent reagent) {
		Entry entry = contents.get(reagent.id);
		return entry == null ? 0 : entry.amount;
	}

	public double getPurity(ChemReagent reagent) {
		Entry entry = contents.get(reagent.id);
		return entry == null ? 0 : entry.purity;
	}

	public double getAveragePurity() {
		double total = getTotalAmount();
		if(total <= 0) return 1.0D;
		double sum = 0;
		for(Entry entry : contents.values()) sum += entry.purity * entry.amount;
		return sum / total;
	}

	public double getTotalAmount() {
		double total = 0;
		for(Entry entry : contents.values()) total += entry.amount;
		return total;
	}

	public double getCapacity() { return capacity; }
	public double getTemperature() { return temperature; }
	public double getPH() { return ph; }
	public Collection<Entry> getContents() { return Collections.unmodifiableCollection(contents.values()); }

	public void changeTemperature(double amount) {
		temperature = Math.max(0, temperature + amount);
	}

	public void changePH(double amount) {
		ph = Math.max(0, Math.min(14, ph + amount));
	}

	public void writeToNBT(NBTTagCompound nbt, String key) {
		NBTTagCompound holder = new NBTTagCompound();
		holder.setDouble("temperature", temperature);
		holder.setDouble("ph", ph);
		NBTTagList list = new NBTTagList();
		for(Entry entry : contents.values()) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", entry.reagent.id);
			tag.setDouble("amount", entry.amount);
			tag.setDouble("purity", entry.purity);
			list.appendTag(tag);
		}
		holder.setTag("contents", list);
		nbt.setTag(key, holder);
	}

	public void readFromNBT(NBTTagCompound nbt, String key) {
		clear();
		if(!nbt.hasKey(key)) return;
		NBTTagCompound holder = nbt.getCompoundTag(key);
		temperature = holder.getDouble("temperature");
		ph = holder.getDouble("ph");
		NBTTagList list = holder.getTagList("contents", 10);
		for(int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			ChemReagent reagent = ChemReagents.get(tag.getString("id"));
			if(reagent != null) putDirect(reagent, tag.getDouble("amount"), tag.getDouble("purity"));
		}
	}

	public void writeToBuffer(ByteBuf buf) {
		buf.writeDouble(temperature);
		buf.writeDouble(ph);
		buf.writeShort(contents.size());
		for(Entry entry : contents.values()) {
			ByteBufUtils.writeUTF8String(buf, entry.reagent.id);
			buf.writeDouble(entry.amount);
			buf.writeDouble(entry.purity);
		}
	}

	public void readFromBuffer(ByteBuf buf) {
		contents.clear();
		temperature = buf.readDouble();
		ph = buf.readDouble();
		int count = buf.readUnsignedShort();
		for(int i = 0; i < count; i++) {
			ChemReagent reagent = ChemReagents.get(ByteBufUtils.readUTF8String(buf));
			double amount = buf.readDouble();
			double purity = buf.readDouble();
			if(reagent != null) putDirect(reagent, amount, purity);
		}
	}

	private void putDirect(ChemReagent reagent, double amount, double purity) {
		if(amount <= 0 || getTotalAmount() >= capacity) return;
		amount = Math.min(amount, capacity - getTotalAmount());
		contents.put(reagent.id, new Entry(reagent, amount, clamp01(purity)));
	}

	private static double clamp01(double value) {
		return Math.max(0, Math.min(1, value));
	}
}
