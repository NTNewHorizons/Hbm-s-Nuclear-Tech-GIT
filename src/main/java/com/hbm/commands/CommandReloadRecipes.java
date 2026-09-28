package com.hbm.commands;

import com.hbm.config.ItemPoolConfigJSON;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.util.ChatBuilder;
import com.hbm.util.DamageResistanceHandler;

import com.hbm.world.gen.util.LogicBlockActions;
import com.hbm.world.gen.util.LogicBlockConditions;
import com.hbm.world.gen.util.LogicBlockInteractions;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentTranslation;

public class CommandReloadRecipes extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmreload";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.reload_recipes.ntmreload";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		try {
			FluidContainerRegistry.clearRegistry(); // we do this first so IFluidRegisterListener can go wild with the registry
			Fluids.reloadFluids();
			FluidContainerRegistry.register();
			SerializableRecipe.initialize();
			ItemPoolConfigJSON.initialize();
			DamageResistanceHandler.init();

			LogicBlockActions.initialize();
			LogicBlockConditions.initialize();
			LogicBlockInteractions.initialize();


			sender.addChatMessage(new ChatComponentTranslation("chat.command_reload_recipes.s_reload_complete", EnumChatFormatting.YELLOW));
		} catch(Exception ex) {
			sender.addChatMessage(ChatBuilder.start("----------------------------------").color(EnumChatFormatting.GRAY).flush());
			sender.addChatMessage(ChatBuilder.startTranslation("chat.command_reload_recipes.an_error_has_occoured_during_loading_consult_the_log_for").color(EnumChatFormatting.RED).flush());
			sender.addChatMessage(ChatBuilder.start(ex.getLocalizedMessage()).color(EnumChatFormatting.RED).flush());
			sender.addChatMessage(ChatBuilder.start(ex.getStackTrace()[0].toString()).color(EnumChatFormatting.RED).flush());
			sender.addChatMessage(ChatBuilder.start("----------------------------------").color(EnumChatFormatting.GRAY).flush());
			throw ex;
		}
	}
}
