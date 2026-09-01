package com.hbm.render.tileentity;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import com.hbm.util.RenderUtil;

import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntitySolarBoiler;
import com.hbm.tileentity.machine.TileEntityFusionTorusStruct;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;

import com.hbm.potion.HbmPotion;

public class RenderHighWrapper extends TileEntitySpecialRenderer implements IItemRendererProvider {

	private final TileEntitySpecialRenderer wrapped;

	private final float[] identity = { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
	private final float[] original = { 1, 0, 0, 0, -0.4f, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
	private final float[] squished = { 1, 0, 0, 0, 0, 0.1f, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
	private final float[] flipped = { 1, 0, 0, 0, 0.4f, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };

	private static double animTime = 0.0D;
	private static double currentAngle = 0.0D;
	private static float currentSpeed = 0.0f;
	private static long lastTime = 0L;

	public RenderHighWrapper(TileEntitySpecialRenderer wrapped) {
		this.wrapped = wrapped;
	}

	private static void updateHighAnimation(boolean isHigh) {
		long now = System.currentTimeMillis();
		if (lastTime == 0L) {
			lastTime = now;
			return;
		}
		long dt = Math.min(100L, now - lastTime);
		if (dt > 0L) {
			lastTime = now;
			float targetSpeed = isHigh ? 1.0f : 0.0f;
			float rampRate = 0.001f * dt;
			if (currentSpeed < targetSpeed) {
				currentSpeed = Math.min(targetSpeed, currentSpeed + rampRate);
			} else if (currentSpeed > targetSpeed) {
				currentSpeed = Math.max(targetSpeed, currentSpeed - rampRate);
			}
			animTime += dt * currentSpeed;
			currentAngle = (currentAngle + dt * 0.36D * currentSpeed) % 360.0D;
		}
	}

	private static boolean shouldBoth(TileEntity te) {
		if (te == null)
			return false;
		String pkg = te.getClass().getPackage() != null ? te.getClass().getPackage().getName() : "";
		return pkg.contains("com.hbm.tileentity.bomb");
	}

	private static boolean shouldOnlySpin(TileEntity te) {
		if (te == null)
			return false;
		// Boilers
		if (te instanceof TileEntityHeatBoiler ||
				te instanceof TileEntityHeatBoilerIndustrial ||
				te instanceof TileEntitySolarBoiler ||
				te instanceof TileEntityFusionBoiler ||
				te instanceof TileEntityRBMKBoiler) {
			return true;
		}
		// Fusion reactor vessel
		if (te instanceof TileEntityFusionTorus ||
				te instanceof TileEntityFusionTorusStruct) {
			return true;
		}
		return false;
	}

	private static boolean shouldSpin(TileEntity te) {
		return shouldBoth(te) || shouldOnlySpin(te);
	}

	private static boolean shouldBounce(TileEntity te) {
		return shouldBoth(te) || !shouldOnlySpin(te);
	}

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float inter) {
		boolean isHigh = Minecraft.getMinecraft().thePlayer != null
				&& Minecraft.getMinecraft().thePlayer.isPotionActive(HbmPotion.high);
		updateHighAnimation(isHigh);

		boolean active = (currentSpeed > 0.0f);
		if (active) {
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5D, y, z + 0.5D);

			if (shouldBounce(te)) {
				double sine = Math.sin(animTime / 200D);
				float[] targetFrame = sine < 0
						? RenderUtil.getLerpedArray(original, squished, (float) (sine + 1.0D))
						: RenderUtil.getLerpedArray(squished, flipped, (float) sine);
				FloatBuffer currentFrame = RenderUtil.lerpMatrix(identity, targetFrame, currentSpeed);
				GL11.glMultMatrix(currentFrame);
			}

			if (shouldSpin(te)) {
				GL11.glRotated(currentAngle, 0D, 1D, 0D);
			}

			GL11.glTranslated(-(x + 0.5D), -y, -(z + 0.5D));
		}

		if (wrapped != null) {
			wrapped.renderTileEntityAt(te, x, y, z, inter);
		}

		if (active) {
			GL11.glPopMatrix();
		}
	}

	/**
	 * MCP obfuscated name for: setRendererDispatcher(TileEntityRendererDispatcher
	 * dispatcher)
	 * Sets the TileEntityRendererDispatcher reference on this renderer and
	 * delegates it to the wrapped renderer
	 * so internal renderer calls (like font rendering or player position lookups)
	 * don't throw NullPointerExceptions.
	 */
	@Override
	public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
		super.func_147497_a(dispatcher);
		if (wrapped != null) {
			wrapped.func_147497_a(dispatcher);
		}
	}

	/**
	 * MCP obfuscated name for: setWorld(World world) / onWorldChange(World world)
	 * Updates the current client World reference and delegates it to the wrapped
	 * renderer
	 * so it stays synchronized when the player switches worlds or dimensions.
	 */
	@Override
	public void func_147496_a(World world) {
		super.func_147496_a(world);
		if (wrapped != null) {
			wrapped.func_147496_a(world);
		}
	}

	@Override
	public Item getItemForRenderer() {
		if (wrapped instanceof IItemRendererProvider) {
			return ((IItemRendererProvider) wrapped).getItemForRenderer();
		}
		return null;
	}

	@Override
	public Item[] getItemsForRenderer() {
		if (wrapped instanceof IItemRendererProvider) {
			return ((IItemRendererProvider) wrapped).getItemsForRenderer();
		}
		return new Item[] { this.getItemForRenderer() };
	}

	@Override
	public IItemRenderer getRenderer() {
		if (wrapped instanceof IItemRendererProvider) {
			return ((IItemRendererProvider) wrapped).getRenderer();
		}
		return null;
	}
}
