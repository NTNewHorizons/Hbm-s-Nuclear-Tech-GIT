package com.hbm.util;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.fluid.trait.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;

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
		if(params.length != 1) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return setFluid(tank, parseFluid(params[0]), validator);
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

	public static FluidType parseFluid(String[] params) {
		if(params.length != 1) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return parseFluid(params[0]);
	}

	private static FluidType parseFluid(String value) {
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
		if(type.hasNoID()) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return type;
	}

	@FunctionalInterface
	public interface FluidValidator {
		void validate(FluidType type);
	}
}
