package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.RefStrings;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.tileentity.network.TileEntityFluidRegulatorValve;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.block.IToolable;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

public class FluidRegulatorValve extends FluidDuctBase implements ILookOverlay, IToolable {

	public FluidRegulatorValve(Material mat) {
		super(mat);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		this.blockIcon = iconRegister.registerIcon(RefStrings.MODID + ":fluid_regulator_valve");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int metadata) {
		return blockIcon;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityFluidRegulatorValve();
	}

	public static void explode(World world, int x, int y, int z) {
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "hbm:block.fart_reverb", 1.0F, 1.0F);

		NBTTagCompound data = new NBTTagCompound();
		data.setString("type", "vanillaExt");
		data.setString("mode", "largeexplode");
		data.setFloat("size", 4.0F);
		data.setByte("count", (byte) 4);
		PacketDispatcher.wrapper.sendToAllAround(
			new AuxParticlePacketNT(data, x + 0.5, y + 0.5, z + 0.5),
			new TargetPoint(world.provider.dimensionId, x + 0.5, y + 0.5, z + 0.5, 100)
		);

		world.func_147480_a(x, y, z, true);
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool == ToolType.SCREWDRIVER) {
			if(world.isRemote) return true;
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntityFluidRegulatorValve) {
				TileEntityFluidRegulatorValve valve = (TileEntityFluidRegulatorValve) te;
				valve.cycleThreshold(player.isSneaking());
				player.addChatComponentMessage(new ChatComponentTranslation("chat.fluid_regulator_valve.threshold", valve.getTargetPercentage()));
				return true;
			}
		}
		return false;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);

		if(!(te instanceof TileEntityFluidRegulatorValve))
			return;

		TileEntityFluidRegulatorValve valve = (TileEntityFluidRegulatorValve) te;

		List<String> text = new ArrayList<>();
		int percent = valve.getTargetPercentage();
		FluidType type = valve.getType();
		text.add("&[" + type.getColor() + "&]" + type.getLocalizedName());

		if(type == Fluids.NONE) {
			text.add(I18nUtil.resolveKey("overlay.fluid_regulator_valve.unconfigured", percent));
		} else if(valve.clientTankMax > 0) {
			long targetMb = valve.getTargetAmount(valve.clientTankMax);
			text.add(I18nUtil.resolveKey("overlay.fluid_regulator_valve.target", percent, BobMathUtil.format(targetMb), BobMathUtil.format(valve.clientTankMax)));
		} else {
			text.add(I18nUtil.resolveKey("overlay.fluid_regulator_valve.no_tank", percent));
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}
}
