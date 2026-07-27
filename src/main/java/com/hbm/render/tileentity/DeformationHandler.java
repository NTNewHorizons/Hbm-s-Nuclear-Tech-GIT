package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

public class DeformationHandler {

	public static void applyDeformation(boolean isWorking) {
		if(isWorking) {
			double sine = Math.sin(System.currentTimeMillis() / 50D % (Math.PI * 2));
			sine *= 0.01D;
			GL11.glScaled(1 - sine, 1 + sine, 1 - sine);
		}
	}
}
