package com.hbm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.fluid.trait.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;

import api.hbm.redstoneoverradio.IRORInfo;
import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.RORFunctionException;

public final class RORFluidUtil {

	public static final String SET_FLUID = IRORInteractive.PREFIX_FUNCTION + "setfluid";
	public static final String SET_FLUID_INFO = SET_FLUID + IRORInteractive.NAME_SEPARATOR + "fluid_name or ID (empty tank)";

	public static final String EX_TANK_EMPTY = "Exception: Tank Must Be Empty";
	public static final String EX_OUTPUT_EMPTY = "Exception: Output Tank Must Be Empty";

	private RORFluidUtil() { }

	/** Returns whether the tank type changed. The machine handles saving and syncing the change. */
	public static boolean setFluid(FluidTank tank, String[] params) {
		return setFluid(tank, params, null);
	}

	/**
	 * Returns whether the tank type changed. The validator runs after the target tank has been confirmed empty,
	 * but before its type is changed, and may reject fluids the machine cannot process.
	 */
	public static boolean setFluid(FluidTank tank, String[] params, FluidValidator validator) {
		return setFluid(tank, params, validator, false);
	}

	/** Allows a caller to explicitly opt into selecting registered FT_NoID fluids. */
	public static boolean setFluid(FluidTank tank, String[] params, boolean allowNoID) {
		return setFluid(tank, params, null, allowNoID);
	}

	/** The opt-in only bypasses FT_NoID; the empty-tank check and validator still apply. */
	public static boolean setFluid(FluidTank tank, String[] params, FluidValidator validator, boolean allowNoID) {
		return setFluid(tank, parseFluid(params, allowNoID), validator);
	}

	/** Returns whether the tank type changed. The machine handles saving and syncing the change. */
	public static boolean setFluid(FluidTank tank, FluidType type) {
		return setFluid(tank, type, null);
	}

	/** Returns whether the tank type changed. The machine handles saving and syncing the change. */
	public static boolean setFluid(FluidTank tank, FluidType type, FluidValidator validator) {
		if(tank.getTankType() == type) return false;
		if(tank.getFill() != 0) throw new RORFunctionException(EX_TANK_EMPTY);
		if(validator != null) validator.validate(type);
		tank.setTankType(type);
		return true;
	}

	/** Throws if changing a tank to the supplied type would void its contents. */
	public static void requireEmptyForTypeChange(FluidTank tank, FluidType type) {
		requireEmptyForTypeChange(tank, type, tank.getMaxFill());
	}

	/** Throws if changing a tank to the supplied type or capacity would void its contents. */
	public static void requireEmptyForTypeChange(FluidTank tank, FluidType type, int newMaxFill) {
		if(tank.getFill() > 0 && (tank.getTankType() != type || tank.getFill() > newMaxFill))
			throw new RORFunctionException(EX_OUTPUT_EMPTY);
	}

	public static void requireGas(FluidType type) {
		if(!type.hasTrait(FT_Gaseous.class) && !type.hasTrait(FT_Gaseous_ART.class))
			throw new RORFunctionException("Exception: Gas Required");
	}

	/** Resolves a player-selectable fluid. Machine-specific eligibility belongs in the caller's validator. */
	public static FluidType parseFluid(String[] params) {
		return parseFluid(params, false);
	}

	/** Resolves a registered fluid, optionally allowing fluids hidden from player selection. */
	public static FluidType parseFluid(String[] params, boolean allowNoID) {
		if(params.length != 1) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return parseFluid(params[0], allowNoID);
	}

	/** Resolves a player-selectable fluid. Machine-specific eligibility belongs in the caller's validator. */
	public static FluidType parseFluid(String value) {
		return parseFluid(value, false);
	}

	/** Resolves a registered fluid, optionally allowing fluids hidden from player selection. */
	public static FluidType parseFluid(String value, boolean allowNoID) {
		String name = value.trim();
		FluidType type;
		try {
			int id = Integer.parseInt(name);
			type = Fluids.fromID(id);
			if(type.getID() != id) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		} catch(NumberFormatException e) {
			type = Fluids.fromName(name);
			if(!type.getName().equals(name)) {
				type = null;
				for(FluidType candidate : Fluids.getAll()) {
					if(candidate.getName().equalsIgnoreCase(name)) {
						type = candidate;
						break;
					}
				}
				if(type == null) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
			}
		}
		if(!allowNoID && type.hasNoID()) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return type;
	}

	@FunctionalInterface
	public interface FluidValidator {
		void validate(FluidType type);
	}

	/** Preserve the unqualified names used by blocks with one tank. */
	public static String[] addSingleTankInfo(String[] info) {
		List<String> result = new ArrayList<>(Arrays.asList(info));
		result.add(IRORInfo.PREFIX_VALUE + "type");
		addFillInfo(result, IRORInfo.PREFIX_VALUE);
		return result.toArray(new String[0]);
	}

	public static String getSingleTankValue(String name, FluidTank tank) {
		return getTankProperties(name, IRORInfo.PREFIX_VALUE, tank);
	}

	public static String[] addTankInfo(String[] info, String role, int count) {
		List<String> result = new ArrayList<>(Arrays.asList(info));
		for(int i = 1; i <= count; i++) {
			String prefix = IRORInfo.PREFIX_VALUE + role + i;
			result.add(prefix + "type");
			addFillInfo(result, prefix);
		}
		return result.toArray(new String[0]);
	}

	public static String[] addBufferInfo(String[] info, String buffer) {
		List<String> result = new ArrayList<>(Arrays.asList(info));
		addFillInfo(result, IRORInfo.PREFIX_VALUE + buffer);
		return result.toArray(new String[0]);
	}

	private static void addFillInfo(List<String> info, String prefix) {
		info.add(prefix + "fill");
		info.add(prefix + "capacity");
		info.add(prefix + "fillpercent");
	}

	/** Returns null for unknown properties or indices. A null tank represents a disabled, zero-capacity tank. */
	public static String getTankValue(String name, String role, FluidTank... tanks) {
		for(int i = 0; i < tanks.length; i++) {
			String prefix = IRORInfo.PREFIX_VALUE + role + (i + 1);
			String value = getTankProperties(name, prefix, tanks[i]);
			if(value != null) return value;
		}
		return null;
	}

	private static String getTankProperties(String name, String prefix, FluidTank tank) {
		if((prefix + "type").equals(name)) return tank == null ? "NONE" : tank.getTankType().getName();
		return getFillValue(name, prefix, tank == null ? 0 : tank.getFill(), tank == null ? 0 : tank.getMaxFill());
	}

	/** Buffer amounts and capacities use the machine's native units, which need not be millibuckets. */
	public static String getBufferValue(String name, String buffer, int fill, int capacity) {
		return getFillValue(name, IRORInfo.PREFIX_VALUE + buffer, fill, capacity);
	}

	private static String getFillValue(String name, String prefix, int fill, int capacity) {
		if((prefix + "fill").equals(name)) return Integer.toString(fill);
		if((prefix + "capacity").equals(name)) return Integer.toString(capacity);
		// Use long arithmetic: some steam tanks overflow an int when their fill is multiplied by 100.
		if((prefix + "fillpercent").equals(name)) return Long.toString(capacity <= 0 ? 0 : (long) fill * 100 / capacity);
		return null;
	}
}
