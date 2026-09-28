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
			if("help".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_shows_usage_for_ntmserver_subcommands", EnumChatFormatting.YELLOW));
			if("list".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_shows_all_server_variable_names_and_values", EnumChatFormatting.YELLOW));
			if("reload".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_reads_server_variables_from_the_config_file", EnumChatFormatting.YELLOW));
			if("get".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_shows_value_for_the_specified_variable_name", EnumChatFormatting.YELLOW));
			if("set".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_sets_a_variable_s_value_and_saves_it_to_the_config_fil", EnumChatFormatting.YELLOW));
		} else {
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_ntmserver_shelp_s_command", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_ntmserver_slist", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_ntmserver_sreload", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_ntmserver_sget_s_name", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_server.s_ntmserver_sset_s_name_value", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
		}
	}
	
	@Override public HashMap<String, ConfigWrapper> getConfigMap() { return ServerConfig.configMap; }
	@Override public void refresh() { ServerConfig.refresh(); }
	@Override public void reload() { ServerConfig.reload(); }
	@Override public String getTitle() { return "SERVER VARIABLES:"; }
}
