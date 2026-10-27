package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityChungusSolar;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderChungusSolar extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);

		GL11.glPushMatrix();

		switch(te.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GL11.glRotatef(90, 0F, 1F, 0F); break;	// north
		case 3: GL11.glRotatef(-90, 0F, 1F, 0F); break;	// south
		case 4: GL11.glRotatef(180, 0F, 1F, 0F); break;	// west
		case 5: GL11.glRotatef(0, 0F, 1F, 0F); break;	// east
		}

		// shifts the model so the port cap sits at the core's outer face, matching the energy port
		GL11.glTranslated(-1.0D, 0, 0);

		GL11.glShadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.chungus_solar_tex);
		ResourceManager.chungus_solar.renderOnly("PortHousing", "Port");
		GL11.glPushMatrix();
		// Roll the array around its lengthwise shaft; the port stays fixed.
		GL11.glTranslated(0, 0.5D, 0);
		GL11.glRotatef(((TileEntityChungusSolar) te).panelRotation * 45F, 1F, 0F, 0F);
		GL11.glTranslated(0, -0.5D, 0);
		ResourceManager.chungus_solar.renderOnly("Array", "Panels");
		GL11.glPopMatrix();
		GL11.glShadeModel(GL11.GL_FLAT);

		GL11.glPopMatrix();

		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.machine_chungus_solar);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			public void renderInventory() {
				GL11.glTranslated(0, 1.25, 0);
				GL11.glScaled(1.25, 1.25, 1.25);
			}
			public void renderCommon() {
				GL11.glRotated(90, 0, 1, 0);
				GL11.glTranslated(7, -0.5, 0); // recenters the 17 block long model
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.chungus_solar_tex);
				ResourceManager.chungus_solar.renderAll();
				GL11.glShadeModel(GL11.GL_FLAT);
				GL11.glEnable(GL11.GL_CULL_FACE);
			}};
	}

}
