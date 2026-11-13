package com.hbm.render.entity.item;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.item.EntitySpidertron;
import com.hbm.util.Vec3NT;

import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

public class RenderSpidertron extends Render {

	private static final float LEG_WIDTH = 0.3F;

	private final ModelBoat body = new ModelBoat();
	private final ModelRenderer cube;

	public RenderSpidertron() {
		ModelBase model = new ModelBase() {};
		this.cube = new ModelRenderer(model, 0, 0);
		this.cube.addBox(0, 0, 0, 1, 1, 1);
	}

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		EntitySpidertron spidertron = (EntitySpidertron) entity;

		// Set color
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_NORMALIZE);
			GL11.glColor4f(1F, 1F, 1F, 1F);

			GL11.glPushMatrix();
				GL11.glTranslated(x, y, z);

				// The boat model uses a different coordinate system from the leg offsets.
				GL11.glPushMatrix();
					GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
					GL11.glScalef(-1.0F, -1.0F, 1.0F);
					this.body.render(entity, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
				GL11.glPopMatrix();
			GL11.glPopMatrix();

			GL11.glPushMatrix();
				GL11.glTranslated(x - spidertron.posX, y - spidertron.posY, z - spidertron.posZ);
				for (EntitySpidertron.Leg leg : spidertron.legs) {
					renderLeg(leg);
				}
			GL11.glPopMatrix();
		GL11.glPopAttrib();
	}

	private void renderLeg(EntitySpidertron.Leg leg) {
		renderSegment(new Vec3NT(leg.hip), leg.knee);
		renderSegment(new Vec3NT(leg.knee), leg.foot);
	}

	private void renderSegment(Vec3NT start, Vec3NT end) {
		/* GL11.glPushMatrix();
			GL11.glTranslated(start.xCoord, start.yCoord, start.zCoord);
		GL11.glPopMatrix(); */
		GL11.glBegin(GL11.GL_LINES);
    		GL11.glColor4d(1.0D, 1.0D, 1.0D, 1.0D);
    		GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
    		GL11.glVertex3d(end.xCoord, end.yCoord, end.zCoord);
		GL11.glEnd();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return new ResourceLocation("hbm:textures/entity/spidertron.png");
	}

	/* private void renderSegment(EntitySpidertron.Leg.Segment segment) {
		Vec3 start = segment.start;
		Vec3 end = segment.end;
		double dx = end.xCoord - start.xCoord;
		double dy = end.yCoord - start.yCoord;
		double dz = end.zCoord - start.zCoord;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double length = Math.sqrt(horizontal * horizontal + dy * dy);
		if(length < 0.0001D) return;

		GL11.glPushMatrix();

		// Aim the cube's positive Y axis along the segment.
		GL11.glTranslated(start.xCoord, start.yCoord, start.zCoord);
		GL11.glRotatef((float) Math.toDegrees(Math.atan2(dx, dz)), 0.0F, 1.0F, 0.0F);
		GL11.glRotatef((float) Math.toDegrees(Math.atan2(horizontal, dy)), 1.0F, 0.0F, 0.0F);

		// Make it not cube
		GL11.glTranslatef(-LEG_WIDTH / 2, 0, -LEG_WIDTH / 2);
		GL11.glScalef(LEG_WIDTH, (float) length, LEG_WIDTH);

		this.cube.render(1);

		GL11.glPopMatrix();
	} */
}
