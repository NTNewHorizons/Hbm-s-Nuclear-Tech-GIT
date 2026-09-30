package com.hbm.commands;

import java.util.HashMap;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentTranslation;

public class CommandReloadServer extends CommandReloadConfig {

	@Override
	public String getCommandName() {
		return "ntmserver";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.reload_server.ntmserver_help";
	}
	
	@Override public void help(ICommandSender sender, String[] args) {
		if(args.length >= 2) {
			String command = args[1];
			if("help".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.help", EnumChatFormatting.YELLOW));
			if("list".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.text.03", EnumChatFormatting.YELLOW));
			if("reload".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.text.01", EnumChatFormatting.YELLOW));
			if("get".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.text.04", EnumChatFormatting.YELLOW));
			if("set".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.text.02", EnumChatFormatting.YELLOW));
		} else {
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.ntmserver_shelp_command", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.ntmserver_slist", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.ntmserver_sreload", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.ntmserver_sget_name", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_server.ntmserver_sset_name_value", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
		}
	}
	
	@Override public HashMap<String, ConfigWrapper> getConfigMap() { return ServerConfig.configMap; }
	@Override public void refresh() { ServerConfig.refresh(); }
	@Override public void reload() { ServerConfig.reload(); }
	@Override public String getTitle() { return "commands.ntmserver.title"; }
}
