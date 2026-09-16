package com.hbm.items.tool;

import baubles.api.BaubleType;
import baubles.api.IBauble;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionData;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PlayerInformPacket;
import com.hbm.util.ChatBuilder;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.common.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles")
public class ItemPollutionDetector extends Item implements IBauble {

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int i, boolean bool) {
		tickDetector(world, entity);
	}

	private void tickDetector(World world, Entity entity) {
		if(!(entity instanceof EntityPlayerMP) || world.getTotalWorldTime() % 10 != 0) return;
		
		PollutionData data = PollutionHandler.getPollutionData(world, (int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ));
		if(data == null) data = new PollutionData();

		float soot = data.pollution[PollutionType.SOOT.ordinal()];
		float poison = data.pollution[PollutionType.POISON.ordinal()];
		float heavymetal = data.pollution[PollutionType.HEAVYMETAL.ordinal()];
		//float fallout = data.pollution[PollutionType.FALLOUT.ordinal()];

		soot = ((int) (soot * 100)) / 100F;
		poison = ((int) (poison * 100)) / 100F;
		heavymetal = ((int) (heavymetal * 100)) / 100F;
		//fallout = ((int) (fallout * 100)) / 100F;

		PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start(I18nUtil.resolveKey("pollution.soot") + ": " + soot).color(EnumChatFormatting.YELLOW).flush(), 100, 4000), (EntityPlayerMP) entity);
		PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start(I18nUtil.resolveKey("pollution.poison") + ": " + poison).color(EnumChatFormatting.YELLOW).flush(), 101, 4000), (EntityPlayerMP) entity);
		PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start(I18nUtil.resolveKey("pollution.heavymetal") + ": " + heavymetal).color(EnumChatFormatting.YELLOW).flush(), 102, 4000), (EntityPlayerMP) entity);
		//PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("Fallout: " + fallout).color(EnumChatFormatting.YELLOW).flush(), 103, 4000), (EntityPlayerMP) entity);
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public BaubleType getBaubleType(ItemStack stack) {
		return BaubleType.BELT;
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public void onWornTick(ItemStack stack, EntityLivingBase entity) {
		tickDetector(entity.worldObj, entity);
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public void onEquipped(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public void onUnequipped(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canEquip(ItemStack stack, EntityLivingBase entity) {
		return true;
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canUnequip(ItemStack stack, EntityLivingBase entity) {
		return true;
	}
}
