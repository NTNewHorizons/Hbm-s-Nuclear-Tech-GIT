package com.hbm.util;

import java.util.Arrays;
import java.util.Locale;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.IFluidCopiable;

import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.RORFunctionException;
import net.minecraft.tileentity.TileEntity;

public class RORFluidControls {

	public static IRORInteractive getController(final TileEntity tile) {
		final IRORInteractive nativeController = tile instanceof IRORInteractive ? (IRORInteractive) tile : null;
		if(!(tile instanceof IFluidCopiable) || ((IFluidCopiable) tile).getTankToPaste() == null) return nativeController;

		return new IRORInteractive() {
			@Override
			public String[] getFunctionInfo() {
				String[] nativeInfo = nativeController == null ? new String[0] : nativeController.getFunctionInfo();
				String[] info = Arrays.copyOf(nativeInfo, nativeInfo.length + 1);
				info[nativeInfo.length] = PREFIX_FUNCTION + "setfluid" + NAME_SEPARATOR + "fluid name or ID (empty tank)";
				return info;
			}

			@Override
			public String runRORFunction(String name, String[] params) {
				if(!(PREFIX_FUNCTION + "setfluid").equals(name)) {
					return nativeController == null ? null : nativeController.runRORFunction(name, params);
				}
				if(params.length != 1) throw new RORFunctionException(EX_FORMAT);
				String fluid = params[0].trim().toUpperCase(Locale.US);
				FluidType type;
				try {
					int id = Integer.parseInt(fluid);
					type = Fluids.fromID(id);
					if(type.getID() != id) throw new RORFunctionException(EX_FORMAT);
				} catch(NumberFormatException e) {
					type = Fluids.fromName(fluid);
					if(!type.getName().equals(fluid)) throw new RORFunctionException(EX_FORMAT);
				}
				if(type.hasNoID()) throw new RORFunctionException(EX_FORMAT);
				FluidTank tank = ((IFluidCopiable) tile).getTankToPaste();
				if(tank == null || tank.getTankType() == type) return null;
				if(tank.getFill() != 0) throw new RORFunctionException("Exception: Tank Must Be Empty");
				tank.setTankType(type);
				tile.markDirty();
				if(tile.getWorldObj() != null) tile.getWorldObj().markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
				return null;
			}
		};
	}
}
