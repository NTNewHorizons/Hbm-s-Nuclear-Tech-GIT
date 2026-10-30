package com.hbm.util;

import java.util.Locale;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;

import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.RORFunctionException;

public final class RORFluidUtil {

	public static final String SET_FLUID = IRORInteractive.PREFIX_FUNCTION + "setfluid";
	public static final String SET_FLUID_INFO = SET_FLUID + IRORInteractive.NAME_SEPARATOR + "fluid_name or ID (empty tank)";

	private RORFluidUtil() { }

	/** Returns whether the tank type changed. The machine handles saving and syncing the change. */
	public static boolean setFluid(FluidTank tank, String[] params) {
		if(params.length != 1) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		FluidType type = parseFluid(params[0]);
		if(tank.getTankType() == type) return false;
		if(tank.getFill() != 0) throw new RORFunctionException("Exception: Tank Must Be Empty");
		tank.setTankType(type);
		return true;
	}

	public static FluidType parseFluid(String[] params) {
		if(params.length != 1) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return parseFluid(params[0]);
	}

	private static FluidType parseFluid(String value) {
		String fluid = value.trim().toUpperCase(Locale.US);
		FluidType type;
		try {
			int id = Integer.parseInt(fluid);
			type = Fluids.fromID(id);
			if(type.getID() != id) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		} catch(NumberFormatException e) {
			type = Fluids.fromName(fluid);
			if(!type.getName().equals(fluid)) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		}
		if(type.hasNoID()) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
		return type;
	}
}
