package com.hbm.blocks.generic;

import com.hbm.blocks.machine.BlockPillar;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.util.ChatComponentTranslation;

public class BlockWriting extends BlockPillar {

	public BlockWriting(Material mat, String top) {
		super(mat, top);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
			
		} else if(!player.isSneaking()) {
			
			ChatStyle red = new ChatStyle().setColor(EnumChatFormatting.RED);
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.you_should_not_have_come_here").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.this_is_not_a_place_of_honor_no_great_deed_is_commemorat").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.nothing_of_value_is_here").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.what_is_here_is_dangerous_and_repulsive").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.we_considered_ourselves_a_powerful_culture_we_harnessed").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.then_we_saw_the_fire_could_burn_within_living_things_unn").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.and_we_were_afraid").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.we_built_great_tombs_to_hold_the_fire_for_one_hundred_th").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.if_this_place_is_opened_the_fire_will_not_be_isolated_fr").setChatStyle(red));
			player.addChatMessage(new ChatComponentTranslation("chat.block_writing.leave_this_place_and_never_come_back").setChatStyle(red));
			return true;
			
		} else {
			return false;
		}
	}
}
