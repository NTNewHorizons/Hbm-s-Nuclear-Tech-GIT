package com.hbm.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.hbm.config.GeneralConfig;

import api.hbm.energymk2.VoltageEnforcement;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandVoltage extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmvoltage";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return EnumChatFormatting.GREEN + "/" + getCommandName() + " <on|off|legacy|warn|strict|status>";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {

		if(args.length == 0 || "status".equalsIgnoreCase(args[0])) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Voltage system: " + EnumChatFormatting.YELLOW + (VoltageEnforcement.isEnabled() ? "ON" : "OFF") + EnumChatFormatting.GOLD + ", enforcement mode: " + EnumChatFormatting.YELLOW + VoltageEnforcement.currentModeName()));
			return;
		}

		if(args.length == 1) {
			String arg = args[0].toLowerCase(Locale.US);
			if("on".equals(arg)) {
				GeneralConfig.enableVoltageSystem = true;
				VoltageEnforcement.resetWarnThrottle();
				func_152373_a(sender, this, "commands.ntmvoltage.on", new Object[0]);
				return;
			}
			if("off".equals(arg)) {
				GeneralConfig.enableVoltageSystem = false;
				VoltageEnforcement.resetWarnThrottle();
				func_152373_a(sender, this, "commands.ntmvoltage.off", new Object[0]);
				return;
			}
			if("legacy".equals(arg) || "warn".equals(arg) || "strict".equals(arg)) {
				GeneralConfig.voltageEnforcement = arg;
				VoltageEnforcement.setMode(arg);
				VoltageEnforcement.resetWarnThrottle();
				func_152373_a(sender, this, "commands.ntmvoltage.set", new Object[] { arg });
				return;
			}
		}

		throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length == 1) {
			return getListOfStringsFromIterableMatchingLastWord(args, Arrays.asList("on", "off", "legacy", "warn", "strict", "status"));
		}
		return null;
	}
}
