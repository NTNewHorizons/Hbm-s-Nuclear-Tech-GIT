package com.hbm.render.block;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.BlockVoltageCable;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.ObjUtil;
import com.hbm.tileentity.network.TileEntityVoltageCable;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

public class RenderCable implements ISimpleBlockRenderingHandler {

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {

		GL11.glPushMatrix();
		Tessellator tessellator = Tessellator.instance;
		IIcon iicon = block.getIcon(0, 0);
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}
		
		GL11.glRotated(180, 0, 1, 0);
		GL11.glScaled(1.25D, 1.25D, 1.25D);
		tessellator.startDrawingQuads();
		ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "Core", iicon, tessellator, 0, false);
		ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "posX", iicon, tessellator, 0, false);
		ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "negX", iicon, tessellator, 0, false);
		ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "posZ", iicon, tessellator, 0, false);
		ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "negZ", iicon, tessellator, 0, false);
		tessellator.draw();

		GL11.glPopMatrix();
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

		Tessellator tessellator = Tessellator.instance;
		IIcon iicon = block.getIcon(0, 0);
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}

		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1, 1, 1);

		boolean pX = connects(block, world, x, y, z, ForgeDirection.EAST);
		boolean nX = connects(block, world, x, y, z, ForgeDirection.WEST);
		boolean pY = connects(block, world, x, y, z, ForgeDirection.UP);
		boolean nY = connects(block, world, x, y, z, ForgeDirection.DOWN);
		boolean pZ = connects(block, world, x, y, z, ForgeDirection.SOUTH);
		boolean nZ = connects(block, world, x, y, z, ForgeDirection.NORTH);
		
		tessellator.addTranslation(x + 0.5F, y + 0.5F, z + 0.5F);

		if(pX && nX && !pY && !nY && !pZ && !nZ)
			ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "CX", iicon, tessellator, 0, true);
		else if(!pX && !nX && pY && nY && !pZ && !nZ)
			ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "CY", iicon, tessellator, 0, true);
		else if(!pX && !nX && !pY && !nY && pZ && nZ)
			ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "CZ", iicon, tessellator, 0, true);
		
		else {
			ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "Core", iicon, tessellator, 0, true);
			if(pX) ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "posX", iicon, tessellator, 0, true);
			if(nX) ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "negX", iicon, tessellator, 0, true);
			if(pY) ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "posY", iicon, tessellator, 0, true);
			if(nY) ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "negY", iicon, tessellator, 0, true);
			if(nZ) ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "posZ", iicon, tessellator, 0, true);
			if(pZ) ObjUtil.renderPartWithIcon((HFRWavefrontObject) ResourceManager.cable_neo, "negZ", iicon, tessellator, 0, true);
		}
		
		tessellator.addTranslation(-x - 0.5F, -y - 0.5F, -z - 0.5F);

		return true;
	}

	private boolean connects(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
		if(block instanceof BlockVoltageCable) {
			TileEntity tile = world.getTileEntity(x, y, z);
			if(tile instanceof TileEntityVoltageCable) return ((TileEntityVoltageCable) tile).isConnected(direction);
		}
		return Library.canConnect(world, x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ, direction);
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}

	@Override
	public int getRenderId() {
		return BlockCable.renderID;
	}
}
