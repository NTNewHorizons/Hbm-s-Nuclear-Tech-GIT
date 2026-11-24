package com.hbm.render.tileentity;

import com.hbm.tileentity.machine.TileEntityMoxer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderMoxer extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		GL11.glRotated(90, 0, 1, 0);
		GL11.glShadeModel(GL11.GL_SMOOTH);

		switch(tile.getBlockMetadata() - BlockDummyable.offset) {
			case 2: GL11.glRotatef(0, 0F, 1F, 0F); break;
			case 4: GL11.glRotatef(90, 0F, 1F, 0F); break;
			case 3: GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 5: GL11.glRotatef(270, 0F, 1F, 0F); break;
		}

		TileEntityMoxer moxer = (TileEntityMoxer) tile;
		float anim = moxer.prevAnim + (moxer.anim - moxer.prevAnim) * interp;

		bindTexture(ResourceManager.moxer_tex);
		ResourceManager.moxer.renderPart("Core");
		if(moxer.frame) ResourceManager.moxer.renderPart("Frame");

		GL11.glPushMatrix();
		GL11.glRotated(-anim * 45 % 360D, 0, 1, 0);
		ResourceManager.moxer.renderPart("Fan");
		GL11.glPopMatrix();

		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.machine_moxer);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {

			public void renderInventory() {
				GL11.glTranslated(0, -1.5, 0);
				GL11.glScaled(3, 3, 3);
			}
			public void renderCommonWithStack(ItemStack item) {
				GL11.glScaled(0.75, 0.75, 0.75);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.moxer_tex);
				ResourceManager.moxer.renderPart("Core");
				ResourceManager.moxer.renderPart("Frame");
				ResourceManager.moxer.renderPart("Fan");
				GL11.glShadeModel(GL11.GL_FLAT);
			}};
	}
}
