package com.hbm.items.machine.customfuel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

// Isotope quanta (72 = one ingot), stored as "inp" material id / amount pairs plus "amount", the total when fresh
public class CustomFuelMix {

	public static final String KEY_MIX = "inp";
	public static final String KEY_FRESH = "amount";

	public static final double FISSION_BURN = 0.8D;
	public static final double BREED_RATE = 0.12D;
	// fissile quanta bred per fissioned quantum at most, below 1 so fuel can't breed more than it burns
	public static final double CONVERSION_RATIO = 0.6D;

	public final int[] amounts = new int[EnumCustomFuel.values().length];
	// the part missing from the fresh total is fission products
	public int fresh;

	public CustomFuelMix() { }

	public CustomFuelMix(List<MaterialStack> materials) {
		for(MaterialStack mat : materials) {
			EnumCustomFuel fuel = EnumCustomFuel.byMaterial(mat.material);
			if(fuel != null && mat.amount > 0) amounts[fuel.ordinal()] += mat.amount;
		}
		this.fresh = total();
	}

	public static boolean hasMix(ItemStack stack) {
		return stack != null && stack.hasTagCompound() && stack.stackTagCompound.hasKey(KEY_MIX);
	}

	public static CustomFuelMix fromStack(ItemStack stack) {
		if(!hasMix(stack)) return null;
		return fromNBT(stack.stackTagCompound);
	}

	public static CustomFuelMix fromNBT(NBTTagCompound nbt) {
		if(nbt == null || !nbt.hasKey(KEY_MIX)) return null;
		CustomFuelMix mix = new CustomFuelMix();
		int[] inp = nbt.getIntArray(KEY_MIX);
		for(int i = 0; i < inp.length / 2; i++) {
			EnumCustomFuel fuel = EnumCustomFuel.byMaterial(Mats.matById.get(inp[i * 2]));
			if(fuel != null && inp[i * 2 + 1] > 0) mix.amounts[fuel.ordinal()] += inp[i * 2 + 1];
		}
		if(mix.total() <= 0) return null;
		mix.fresh = Math.max(nbt.getInteger(KEY_FRESH), mix.total());
		return mix;
	}

	public ItemStack writeTo(ItemStack stack) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		writeNBT(stack.stackTagCompound);
		return stack;
	}

	public NBTTagCompound writeNBT(NBTTagCompound nbt) {
		List<Integer> inp = new ArrayList();
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			int amount = amounts[fuel.ordinal()];
			if(amount <= 0) continue;
			inp.add(fuel.stack.id);
			inp.add(amount);
		}
		int[] arr = new int[inp.size()];
		for(int i = 0; i < arr.length; i++) arr[i] = inp.get(i);
		nbt.setIntArray(KEY_MIX, arr);
		nbt.setInteger(KEY_FRESH, Math.max(fresh, total()));
		return nbt;
	}

	public void serialize(ByteBuf buf) {
		for(int a : amounts) buf.writeInt(a);
		buf.writeInt(fresh);
	}

	public static CustomFuelMix deserialize(ByteBuf buf) {
		CustomFuelMix mix = new CustomFuelMix();
		for(int i = 0; i < mix.amounts.length; i++) mix.amounts[i] = buf.readInt();
		mix.fresh = buf.readInt();
		return mix;
	}

	public CustomFuelMix copy() {
		CustomFuelMix mix = new CustomFuelMix();
		System.arraycopy(amounts, 0, mix.amounts, 0, amounts.length);
		mix.fresh = fresh;
		return mix;
	}

	public int get(EnumCustomFuel fuel) {
		return amounts[fuel.ordinal()];
	}

	public int total() {
		int sum = 0;
		for(int a : amounts) sum += a;
		return sum;
	}

	public double share(EnumCustomFuel fuel) {
		int total = total();
		return total <= 0 ? 0D : (double) amounts[fuel.ordinal()] / total;
	}

	public double fissileShare() {
		double sum = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) if(fuel.fissile) sum += share(fuel);
		return sum;
	}

	public double fertileShare() {
		return 1D - fissileShare();
	}

	public double fissileShareOf(EnumCustomFuel fuel) {
		double fissile = fissileShare();
		return fissile <= 0 || !fuel.fissile ? 0D : share(fuel) / fissile;
	}

	public double schrabidicShare() {
		return share(EnumCustomFuel.SA326) + share(EnumCustomFuel.SA327);
	}

	public EnumCustomFuel dominantFissile() {
		EnumCustomFuel best = null;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			if(!fuel.fissile || amounts[fuel.ordinal()] <= 0) continue;
			if(best == null || amounts[fuel.ordinal()] > amounts[best.ordinal()]) best = fuel;
		}
		return best != null ? best : EnumCustomFuel.U235;
	}

	public int fissileDiversity() {
		int count = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) if(fuel.fissile && share(fuel) >= 0.05D) count++;
		return count;
	}

	public CustomFuelMix add(CustomFuelMix other) {
		for(int i = 0; i < amounts.length; i++) amounts[i] += other.amounts[i];
		fresh += other.fresh;
		return this;
	}

	public CustomFuelMix divide(int parts) {
		CustomFuelMix mix = new CustomFuelMix();
		for(int i = 0; i < amounts.length; i++) mix.amounts[i] = amounts[i] / parts;
		mix.fresh = fresh / parts;
		return mix;
	}

	// quanta each fertile isotope has bred at this depletion. Fuel with fissile isotopes breeds at most
	// CONVERSION_RATIO per fissioned quantum, so it always ends up with less fissile material than it started with.
	// Fuel without any only breeds from outside flux and keeps the full rate.
	private int[] bred(double depletion) {
		double fissioned = 0, potential = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			int amount = amounts[fuel.ordinal()];
			if(fuel.fissile) fissioned += amount * FISSION_BURN * depletion;
			else if(fuel.breedsInto() != null) potential += amount * BREED_RATE * depletion;
		}

		int[] bred = new int[amounts.length];
		if(potential <= 0) return bred;
		double scale = fissioned > 0 ? Math.min(1D, CONVERSION_RATIO * fissioned / potential) : 1D;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			if(fuel.fissile || fuel.breedsInto() == null) continue;
			bred[fuel.ordinal()] = (int) Math.round(amounts[fuel.ordinal()] * BREED_RATE * depletion * scale);
		}
		return bred;
	}

	// depletion 0 to 1: fissile isotopes fission away, fertile ones breed into fissile ones
	public CustomFuelMix burnUp(double depletion) {
		depletion = Math.max(0D, Math.min(1D, depletion));
		CustomFuelMix mix = copy();
		int[] bred = bred(depletion);
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			int amount = amounts[fuel.ordinal()];
			if(fuel.fissile) {
				mix.amounts[fuel.ordinal()] -= (int) Math.round(amount * FISSION_BURN * depletion);
			} else if(bred[fuel.ordinal()] > 0) {
				mix.amounts[fuel.ordinal()] -= bred[fuel.ordinal()];
				mix.amounts[fuel.breedsInto().ordinal()] += bred[fuel.ordinal()];
			}
		}
		for(int i = 0; i < mix.amounts.length; i++) mix.amounts[i] = Math.max(mix.amounts[i], 0);
		return mix;
	}

	// hybrid model: what has bred while in the core, nothing burned
	public CustomFuelMix withBreeding(double depletion) {
		CustomFuelMix mix = copy();
		int[] bred = bred(Math.max(0D, Math.min(1D, depletion)));
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			if(bred[fuel.ordinal()] <= 0) continue;
			mix.amounts[fuel.ordinal()] -= bred[fuel.ordinal()];
			mix.amounts[fuel.breedsInto().ordinal()] += bred[fuel.ordinal()];
		}
		return mix;
	}

	public List<MaterialStack> toMaterials() {
		List<MaterialStack> list = new ArrayList();
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			if(amounts[fuel.ordinal()] > 0) list.add(new MaterialStack(fuel.stack, amounts[fuel.ordinal()]));
		}
		return list;
	}

	public int getColor() {
		int total = total();
		if(total <= 0) return 0xffffff;
		double r = 0, g = 0, b = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) {
			int amount = amounts[fuel.ordinal()];
			if(amount <= 0) continue;
			double perc = (double) amount / total;
			NTMMaterial mat = fuel.stack;
			r += ((mat.moltenColor & 0xff0000) >> 16) * perc;
			g += ((mat.moltenColor & 0x00ff00) >> 8) * perc;
			b += (mat.moltenColor & 0x0000ff) * perc;
		}
		return ((int) r) << 16 | ((int) g) << 8 | (int) b;
	}

	// fission products are far hotter than the fuel itself
	public float getRadiation() {
		float rad = 0;
		for(EnumCustomFuel fuel : EnumCustomFuel.values()) rad += fuel.radiation * amounts[fuel.ordinal()] / 72F;
		int products = Math.max(fresh - total(), 0);
		rad += products / 72F * 20F;
		return rad;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof CustomFuelMix)) return false;
		return Arrays.equals(amounts, ((CustomFuelMix) o).amounts);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(amounts);
	}
}
