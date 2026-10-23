package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.tileentity.machine.TileEntityChungusSolar;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineChungusSolar extends BlockDummyable implements ITooltipProvider, ILookOverlay {

	public MachineChungusSolar(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityChungusSolar();
		return null;
	}

	/** Horizontal only, 18 blocks long (with the port cap at the facing end) and 3 blocks wide */
	@Override
	public int[] getDimensions() {
		return new int[] { 0, 0, 17, 0, 1, 1 };
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public int[][] getAllPorts(ForgeDirection dir) {
		return rotatePorts(new int[][] {
			{ 0, 0, 0, ForgeDirection.SOUTH.ordinal() },
		}, dir);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		this.addStandardInfo(stack, player, list, ext);
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = findCore(world, x, y, z);
		if(pos == null) return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityChungusSolar)) return;

		TileEntityChungusSolar solar = (TileEntityChungusSolar) te;

		List<String> text = new ArrayList<>();
		text.add(EnumChatFormatting.GREEN + "-> " + EnumChatFormatting.RESET + BobMathUtil.getShortNumber(solar.getPower()) + " / " + BobMathUtil.getShortNumber(solar.getMaxPower()) + "HE");
		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xff8000, 0x404000, text);
	}
}
