package com.hbm.commands;

import java.util.HashMap;

import com.hbm.config.ClientConfig;
import com.hbm.config.RunningConfig.ConfigWrapper;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraft.util.ChatComponentTranslation;

public class CommandReloadClient extends CommandReloadConfig {

	public static void register() {
		if(FMLLaunchHandler.side() != Side.CLIENT) return;
		ClientCommandHandler.instance.registerCommand(new CommandReloadClient());
	}

	@Override
	public String getCommandName() {
		return "ntmclient";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.reload_client.ntmclient_help";
	}
	
	@Override public void help(ICommandSender sender, String[] args) {
		if(args.length >= 2) {
			String command = args[1];
			if("help".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_shows_usage_for_ntmclient_subcommands", EnumChatFormatting.YELLOW));
			if("list".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_shows_all_client_variable_names_and_values", EnumChatFormatting.YELLOW));
			if("reload".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_reads_client_variables_from_the_config_file", EnumChatFormatting.YELLOW));
			if("get".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_shows_value_for_the_specified_variable_name", EnumChatFormatting.YELLOW));
			if("set".equals(command)) sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_sets_a_variable_s_value_and_saves_it_to_the_config_fil", EnumChatFormatting.YELLOW));
		} else {
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_ntmclient_shelp_s_command", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_ntmclient_slist", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_ntmclient_sreload", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_ntmclient_sget_s_name", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_client.s_ntmclient_sset_s_name_value", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
		}
	}
	
	@Override public HashMap<String, ConfigWrapper> getConfigMap() { return ClientConfig.configMap; }
	@Override public void refresh() { ClientConfig.refresh(); }
	@Override public void reload() { ClientConfig.reload(); }
	@Override public String getTitle() { return "commands.ntmclient.title"; }
}
