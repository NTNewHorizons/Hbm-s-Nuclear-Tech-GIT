package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.gui.GUIEnergyConverter;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.network.TileEntityConverterRfHe;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockConverterRfHe extends BlockContainer implements ILookOverlay, IGUIProvider {

	public BlockConverterRfHe(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityConverterRfHe();
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float fX, float fY, float fZ) {
		if(player.isSneaking()) return false;
		if(!(world.getTileEntity(x, y, z) instanceof TileEntityConverterRfHe)) return false;
		if(world.isRemote) player.openGui(MainRegistry.instance, 0, world, x, y, z);
		return true;
	}

	@Override public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIEnergyConverter((TileEntityConverterRfHe) world.getTileEntity(x, y, z)); }
	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return null; }

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);

		if(!(tile instanceof TileEntityConverterRfHe)) return;

		TileEntityConverterRfHe converter = (TileEntityConverterRfHe) tile;
		List<String> text = new ArrayList<>();

		text.add((EnumChatFormatting.GREEN) + "-> " + EnumChatFormatting.RESET + I18nUtil.resolveKey("overlay.converter_rf_he.rf", BobMathUtil.getShortNumber(converter.getEnergyStored(ForgeDirection.UNKNOWN))));
		text.add((EnumChatFormatting.RED) + "<- " + EnumChatFormatting.RESET + ("" + (BobMathUtil.getShortNumber(converter.getPower())) + "HE"));

		text.add(I18nUtil.resolveKey("overlay.converter.max_buffer", BobMathUtil.getShortNumber(converter.getMaxPower())));
		text.add(I18nUtil.resolveKey("overlay.converter.configure"));

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}
}
