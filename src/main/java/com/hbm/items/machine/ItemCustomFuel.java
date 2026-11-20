package com.hbm.items.machine;

import java.util.List;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.util.function.Function;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

public class ItemCustomFuel extends Item {

	protected IIcon iconBG;

	public Item item;

	public static enum EnumCustomFuel {

		TH232(-0.20D, Mats.MAT_THORIUM,    new Function.FunctionLogarithmic(1.00D)),
		U233(  0.15D, Mats.MAT_U233,        new Function.FunctionSqrt(1.00D)),
		U235(  0.10D, Mats.MAT_U235,        new Function.FunctionSqrt(1.00D)),
		U238( -1.00D, Mats.MAT_U238,        new Function.FunctionSqrt(1.25D)),
		PU239( 0.10D, Mats.MAT_PU239,       new Function.FunctionSqrt(1.50D)),
		NP237( 0.10D, Mats.MAT_NEPTUNIUM,   new Function.FunctionSqrt(1.75D)),
		SB326( 0.20D, Mats.MAT_SCHRABIDIUM, new Function.FunctionLinear(2.00D)),
		SB327( 0.50D, Mats.MAT_SOLINIUM,    new Function.FunctionLinear(1.25D)),
		;

		public double selfInfluence;
		public NTMMaterial stack;
		public Function function;

		private EnumCustomFuel(double self, NTMMaterial stack, Function function) {
			this.selfInfluence = self;
			this.stack = stack;
			this.function = function;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		/*
		List<Mats.MaterialStack> stack = new ArrayList();
		stack.add(new Mats.MaterialStack(Mats.matById.get(EnumCustomFuel.U235.stack.id), 1));
		stack.add(new Mats.MaterialStack(Mats.matById.get(EnumCustomFuel.U238.stack.id), 71));
		list.add(this.setup(this.item, stack));
		 */
	}

	public ItemCustomFuel(Item input) {
		this.setMaxStackSize(64);
		this.item = input;
	}

	public static ItemStack setup(Item input, List<Mats.MaterialStack> stack, int amount) {
		return setup(new ItemStack(input), stack, amount);
	}

	public static ItemStack setup(ItemStack stack, List<Mats.MaterialStack> mats, int amount) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		int[] inp = new int[mats.size() * 2];
		String funcName = "";
		for(int i = 0; i < mats.size(); i++) {
			Mats.MaterialStack sta = mats.get(i);
			inp[i * 2] = sta.material.id;
			inp[i * 2 + 1] = sta.amount;
			for (EnumCustomFuel fuel : EnumCustomFuel.values()) {
				if (fuel.stack == sta.material) {
					funcName += String.format("%.2f * (%s) + |", (double) sta.amount / amount, fuel.function.getLabelForFuel());
					break;
				}
			}
		}
		stack.stackTagCompound.setIntArray("inp", inp);
		stack.stackTagCompound.setInteger("amount", amount);
		stack.stackTagCompound.setString("funcName", funcName);
		return stack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		return pass == 1 ? super.getIconFromDamageForRenderPass(meta, pass) : this.iconBG;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if (stack != null && stack.hasTagCompound() && pass == 0) {
			int[] inp = stack.stackTagCompound.getIntArray("inp");

			int size = inp.length / 2;
			if (size > 0) {
				int r = 0;
				int g = 0;
				int b = 0;
				for (int i = 0; i < size; i++) {
					NTMMaterial mat = Mats.matById.get(inp[i * 2]);
					if (mat == null) continue;
					double perc = (double) inp[i * 2 + 1] / Math.max(stack.stackTagCompound.getInteger("amount"), 1);
					r += (mat.moltenColor & 0xff0000) >> 16;
					g += (mat.moltenColor & 0x00ff00) >> 8;
					b += mat.moltenColor & 0x0000ff;
				}
				r /= size;
				g /= size;
				b /= size;
				return r << 16 | g << 8 | b;
			}
		}
		return 0xffffff;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		this.iconBG = reg.registerIcon(this.iconString + "_bg");
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		if (stack != null && stack.hasTagCompound()) {
			int[] inp = stack.stackTagCompound.getIntArray("inp");
			String funcName = stack.stackTagCompound.getString("funcName");
			String funcStep = "";
			for (int i = 0; i < inp.length / 2; i++) {
				NTMMaterial mat = Mats.matById.get(inp[i * 2]);
				if (mat == null) continue;
				list.add(EnumChatFormatting.YELLOW + String.format("(%5.1f%s) %s: %d mB", (double) inp[i * 2 + 1] / Math.max(stack.stackTagCompound.getInteger("amount"), 1) * 100, "%", I18nUtil.resolveKey(mat.getUnlocalizedName()), 2 * inp[i * 2 + 1]));
			}
			list.add(EnumChatFormatting.GREEN + "f(x) = ");
			for (int i = 0; i < funcName.length(); i++) {
				if (funcName.charAt(i) == '|') {
					list.add(EnumChatFormatting.GREEN + funcStep);
					funcStep = "";
				} else {
					funcStep += funcName.charAt(i);
				}
			}
		}
	}
}
