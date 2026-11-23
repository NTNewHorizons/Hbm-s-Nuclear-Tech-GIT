package com.hbm.commands;

import java.util.List;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialTime;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.DimensionManager;
import net.minecraft.world.World;

public class CommandTimeNTM extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmtime";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.ntmtime.usage";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		World overworld = DimensionManager.getWorld(0);
		if(overworld == null || args.length < 1 || !"daylength".equals(args[0])) {
			throw new WrongUsageException(getCommandUsage(sender));
		}

		CelestialTime data = CelestialTime.get(overworld);

		if(args.length == 1) {
			if(data.dayLengthOverride > 0) {
				sender.addChatMessage(new ChatComponentTranslation("commands.ntmtime.query", format(data.getDayLengthMinutes())));
			} else {
				sender.addChatMessage(new ChatComponentTranslation("commands.ntmtime.query_config", format(data.getDayLengthMinutes())));
			}
			return;
		}

		if("reset".equals(args[1])) {
			CelestialTime.setDayLength(overworld, -1);
			sender.addChatMessage(new ChatComponentTranslation("commands.ntmtime.reset", format(SpaceConfig.dayLengthMinutes)));
			return;
		}

		double minutes = parseDoubleBounded(sender, args[1], 1.0D, 100_000.0D);
		CelestialTime.setDayLength(overworld, minutes);
		func_152373_a(sender, this, "commands.ntmtime.set", format(minutes)); // notifyAdmins
	}

	private static String format(double minutes) {
		return minutes == Math.rint(minutes) ? String.valueOf((long) minutes) : String.valueOf(minutes);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length == 1) return getListOfStringsMatchingLastWord(args, "daylength");
		if(args.length == 2 && "daylength".equals(args[0])) return getListOfStringsMatchingLastWord(args, "reset", "20", "40", "60");
		return null;
	}

}
