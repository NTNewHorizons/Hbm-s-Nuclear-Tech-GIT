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
			if("help".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.help", EnumChatFormatting.YELLOW));
			if("list".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.shows_variable_names_values", EnumChatFormatting.YELLOW));
			if("reload".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.reads_variables_config_file", EnumChatFormatting.YELLOW));
			if("get".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.shows_value_specified_variable", EnumChatFormatting.YELLOW));
			if("set".equals(command)) sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.text.02", EnumChatFormatting.YELLOW));
		} else {
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.ntmclient_shelp_command", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.ntmclient_slist", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.ntmclient_sreload", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.ntmclient_sget_name", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
			sender.addChatMessage(new ChatComponentTranslation("commands.reload_client.ntmclient_sset_name_value", EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED));
		}
	}

	@Override public HashMap<String, ConfigWrapper> getConfigMap() { return ClientConfig.configMap; }
	@Override public void refresh() { ClientConfig.refresh(); }
	@Override public void reload() { ClientConfig.reload(); }
	@Override public String getTitle() { return "commands.ntmclient.title"; }
}
