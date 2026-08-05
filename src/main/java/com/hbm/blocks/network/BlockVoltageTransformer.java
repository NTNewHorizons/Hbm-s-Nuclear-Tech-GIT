package com.hbm.blocks.network;

import java.util.List;

import api.hbm.energymk2.TransformerProperties;
import api.hbm.energymk2.VoltageTier;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.network.TileEntityVoltageTransformer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockVoltageTransformer extends BlockContainer implements ITooltipProvider {

	private final TransformerProperties properties;
	@SideOnly(Side.CLIENT) private IIcon inputIcon;
	@SideOnly(Side.CLIENT) private IIcon outputIcon;

	public BlockVoltageTransformer(Material material, TransformerProperties properties) {
		super(material);
		this.properties = properties;
	}

	public TransformerProperties getProperties() {
		return properties;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityVoltageTransformer();
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, net.minecraft.item.ItemStack stack) {
		world.setBlockMetadataWithNotify(x, y, z, BlockPistonBase.determineOrientation(world, x, y, z, player), 2);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		inputIcon = register.registerIcon(RefStrings.MODID + ":pwr_port");
		outputIcon = register.registerIcon(RefStrings.MODID + ":machine_transformer_top_iron");
		blockIcon = register.registerIcon(RefStrings.MODID + ":machine_transformer_iron");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		ForgeDirection input = ForgeDirection.getOrientation(meta);
		if(input == ForgeDirection.UNKNOWN) input = ForgeDirection.NORTH;
		ForgeDirection queriedSide = ForgeDirection.getOrientation(side);
		if(queriedSide == input) return inputIcon;
		if(queriedSide == input.getOpposite()) return outputIcon;
		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(net.minecraft.item.ItemStack stack, EntityPlayer player, List list, boolean ext) {
		list.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted("hbm.voltage.transformerInput", VoltageTier.format(properties.inputVoltage), properties.maxInputEnergyPerTick));
		list.add(EnumChatFormatting.GREEN + StatCollector.translateToLocalFormatted("hbm.voltage.transformerOutput", VoltageTier.format(properties.outputVoltage), properties.getMaxOutputEnergyPerTick()));
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("hbm.voltage.transformerPlacementHint"));
	}
}
