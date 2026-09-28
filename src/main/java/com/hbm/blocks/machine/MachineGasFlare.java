package com.hbm.blocks.machine;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import com.hbm.util.i18n.I18nUtil;

public class MachineGasFlare extends BlockDummyable implements ITooltipProvider {

	public MachineGasFlare(Material mat) {
		super(mat);

		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5D, 0D, -1.5D, 1.5D, 3.875D, 1.5D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.75D, 3.875D, -0.75D, 0.75D, 9, 0.75D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5D, 9D, -1.5D, 1.5D, 9.375D, 1.5D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.75D, 9.375D, -0.75D, 0.75D, 12, 0.75D));
		this.maxY = 0.999D; //item bounce prevention
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		
		if(meta >= 12) return new TileEntityMachineGasFlare();
		if(meta >= 6) return new TileEntityProxyCombo(false, true, true);
		return null;
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return this.standardOpenBehavior(world, x, y, z, player, 0);
	}

	@Override
	public int[] getDimensions() {
		return new int[] {11, 0, 1, 1, 1, 1};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o);
		this.makeExtra(world, x + dir.offsetX * o, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o, y, z + dir.offsetZ * o - 1);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {

		list.add(I18nUtil.resolveKey("tooltip.machine_gas_flare.s_can_burn_fluids_and_vent_gasses", EnumChatFormatting.GOLD));
		list.add(I18nUtil.resolveKey("tooltip.machine_gas_flare.s_burns_up_to_s10m_b_t", EnumChatFormatting.GOLD, EnumChatFormatting.RED));
		list.add(I18nUtil.resolveKey("tooltip.machine_gas_flare.s_vents_up_to_s50m_b_t", EnumChatFormatting.GOLD, EnumChatFormatting.RED));
		list.add("");
		list.add(I18nUtil.resolveKey("tooltip.machine_gas_flare.s_fuel_efficiency", EnumChatFormatting.YELLOW));
		list.add(I18nUtil.resolveKey("tooltip.machine_gas_flare.s_flammable_gasses_s20", EnumChatFormatting.YELLOW, EnumChatFormatting.RED));
		list.add(I18nUtil.resolveKey("tooltip.machine_gas_flare.s_flammable_liquids_s10", EnumChatFormatting.YELLOW, EnumChatFormatting.RED));
	}
}
