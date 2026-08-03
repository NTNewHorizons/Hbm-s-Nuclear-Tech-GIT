package com.hbm.render.tileentity;

import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.render.util.SmallBlockPronter;
import com.hbm.tileentity.machine.TileEntityCustomMachine;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;


public class RenderCustomMachine extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {

		TileEntityCustomMachine custom = (TileEntityCustomMachine) tile;
		CustomMachineConfigJSON.MachineConfiguration config = custom.config;

		ForgeDirection dir = ForgeDirection.getOrientation(tile.getBlockMetadata());
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		if(config != null) {

			if(!custom.structureOK){
				GL11.glPushMatrix();
				try {
					GL11.glTranslated(x, y, z);
					bindTexture(TextureMap.locationBlocksTexture);
					SmallBlockPronter.startDrawing();

					int animIndex = custom.ghostAnimationIndex;
					if(config.components != null && !config.components.isEmpty()) {
						for(CustomMachineConfigJSON.MachineConfiguration.ComponentDefinition compDef : config.components) {
							int rx = -dir.offsetX * compDef.x + rot.offsetX * compDef.x;
							int ry = compDef.y;
							int rz = -dir.offsetZ * compDef.z + rot.offsetZ * compDef.z;
							if(dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
								rx = dir.offsetZ * compDef.z - rot.offsetZ * compDef.z;
								rz = dir.offsetX * compDef.x - rot.offsetX * compDef.x;
							}
							int metaValue = compDef.metaList != null && !compDef.metaList.isEmpty()
								? compDef.metaList.get(animIndex % compDef.metaList.size()) : 0;
							SmallBlockPronter.drawSmolBlockAt(compDef.block, metaValue, rx, ry, rz);
						}
					}

					SmallBlockPronter.draw();
				} catch(Exception e) {
					MainRegistry.logger.error("Error rendering ghost blocks for custom machine", e);
				} finally {
					GL11.glPopMatrix();
				}
			}
			else if(config.customModel != null){
				GL11.glPushMatrix();
				try {
					IModelCustom customModel = MachineModelRegistry.getModel(config.unlocalizedName);
					if(customModel == null) {
						MainRegistry.logger.warn("Model not found for custom machine: " + config.unlocalizedName);
						GL11.glPopMatrix();
						return;
					}
					ResourceLocation modelTexture = new ResourceLocation(RefStrings.MODID, config.customModel.modelTexture);
					double rx = -dir.offsetX * (config.customModel.model_x) + 0.5;
					double ry = +(config.customModel.model_y);
					double rz = -dir.offsetZ * (config.customModel.model_z) + 0.5;
					if(dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
						rx = -dir.offsetX * (config.customModel.model_z) + 0.5;
						rz = -dir.offsetZ * (config.customModel.model_x) + 0.5;
					}
					GL11.glTranslated(x + rx, y + ry, z + rz);

					switch(tile.getBlockMetadata()) {
						case 3: GL11.glRotatef(0, 0F, 1F, 0F); break;
						case 5: GL11.glRotatef(90, 0F, 1F, 0F); break;
						case 2: GL11.glRotatef(180, 0F, 1F, 0F); break;
						case 4: GL11.glRotatef(270, 0F, 1F, 0F); break;
					}
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_CULL_FACE);

					GL11.glShadeModel(GL11.GL_SMOOTH);
					bindTexture(modelTexture);
					customModel.renderAll();
					GL11.glShadeModel(GL11.GL_FLAT);
				} catch(Exception e) {
					MainRegistry.logger.error("Error rendering custom machine model for " + config.unlocalizedName, e);
				} finally {
					GL11.glPopMatrix();
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glDisable(GL11.GL_CULL_FACE);
					GL11.glShadeModel(GL11.GL_FLAT);
				}
			}
		}
	}
}
