package com.hbm.items.tool;

import baubles.api.BaubleType;
import baubles.api.IBauble;

import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.handler.atmosphere.ChunkAtmosphereManager;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PlayerInformPacket;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ChatBuilder;

import cpw.mods.fml.common.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles")
public class ItemAtmosphereScanner extends Item implements IBauble {

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean inHand) {
		tickScanner(world, entity);
	}

	private void tickScanner(World world, Entity entity) {
		if(!(entity instanceof EntityPlayerMP) || world.getTotalWorldTime() % 5 != 0) return;

		EntityPlayerMP player = (EntityPlayerMP) entity;

		CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(entity);

		boolean hasAtmosphere = false;
		if(atmosphere != null) {
			for(int i = 0; i < atmosphere.fluids.size(); i++) {
				FluidEntry entry = atmosphere.fluids.get(i);
				if(entry.pressure > 0.0001) {
					String pressure = String.format("%.4f", BobMathUtil.roundDecimal(entry.pressure, 4));
					PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.startTranslation(entry.fluid.getUnlocalizedName()).color(EnumChatFormatting.AQUA).next(": ").next(pressure + "atm").color(EnumChatFormatting.RESET).flush(), 969 + i, 4000), player);
					hasAtmosphere = true;
				}
			}
		}

		if(!hasAtmosphere) {
			PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("NEAR VACUUM").color(EnumChatFormatting.YELLOW).flush(), 969, 4000), player);
		}
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public BaubleType getBaubleType(ItemStack stack) { return BaubleType.BELT; }

	@Override
	@Optional.Method(modid = "Baubles")
	public void onWornTick(ItemStack stack, EntityLivingBase entity) { tickScanner(entity.worldObj, entity); }

	@Override
	@Optional.Method(modid = "Baubles")
	public void onEquipped(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public void onUnequipped(ItemStack stack, EntityLivingBase entity) { }

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canEquip(ItemStack stack, EntityLivingBase entity) { return true; }

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canUnequip(ItemStack stack, EntityLivingBase entity) { return true; }

}
