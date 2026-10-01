package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.pattern.CameraImpulse;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ScreenFlash;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ShockwavePattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import org.jetbrains.annotations.Nullable;

/**
 * Client factories for Regulus's one-shot VFX events ({@code RegulusVfxIds}).
 * The {@code regulus/anim/<clip>} ids are clip-starts for the authored EMF
 * animation pack — the clip states themselves are owned by the EMF integration
 * (Task 9); until then these factories return a one-tick {@code DoneFx}.
 */
public final class RegulusFx {
	private RegulusFx() {
	}

	public static void register(HeroClientContext ctx) {
		ctx.vfx(RegulusVfxIds.ANIM_DEBRIS_KICK, RegulusFx::clipOnly);
		ctx.vfx(RegulusVfxIds.DEBRIS_IMPACT, RegulusFx::clipOnly);
	}

	/**
	 * Clip-start events with no own visual yet: the EMF clip states land with the
	 * Veil/EMF task — until then a one-tick done effect keeps the contract alive.
	 * Also used for {@code evangelium_major} (the authored hit-frame event).
	 */
	public static VfxEffect clipOnly(VfxSpawn spawn) {
		return new DoneFx();
	}

	/**
	 * The counter slam's visual-only impact ({@code regulus/counter_slam_impact}):
	 * a LOS- and distance-attenuated {@link ScreenFlash}, a warm core light fading
	 * over ~18 ticks, one expanding ring, a distortion pulse and a camera impulse.
	 * No damage lives here — the hit was dealt once, server-side, at SLAM entry.
	 */
	public static VfxEffect counterSlam(VfxSpawn spawn) {
		return new SlamFx(spawn);
	}

	private static final class SlamFx implements VfxEffect {
		private final Vec3 center;
		private final ShockwavePattern ring;
		private final int lightFadeTicks;
		private final int lightRgb;
		private final float lightRadius;
		private final float lightBrightness;

		private int age;
		private @Nullable LightHandle light;
		private boolean finished;

		private SlamFx(VfxSpawn spawn) {
			this.center = spawn.origin();
			VfxParams p = spawn.params();
			float scale = Math.max(0.1f, Math.min(1f, spawn.scale()));
			float att = attenuation(center) * scale;

			ScreenFlash.trigger(att * p.number("flashIntensity", 0.7f),
					p.color("flashColor", 0xFFFFF0D0),
					Math.max(1, (int) p.number("flashTicks", 18f)));

			VfxBackend backend = VfxBackends.current();
			this.lightFadeTicks = Math.max(1, (int) p.number("lightFadeTicks", 18f));
			this.lightRgb = p.color("lightColor", 0xFFE6B8) & 0xFFFFFF;
			this.lightRadius = p.number("lightRadius", 24f) * scale;
			this.lightBrightness = p.number("lightBrightness", 1.8f);
			this.light = backend.light(center, lightRgb, lightRadius, lightBrightness);

			this.ring = new ShockwavePattern(center,
					p.number("ringRadius", 6f) * scale,
					Math.max(1, (int) p.number("ringTicks", 12f)),
					p.color("ringColor", 0xB0FFD9A0), p.number("ringBand", 0.2f));
			backend.distortion(center, p.number("distortionRadius", 8f) * scale,
					p.number("distortionStrength", 0.6f) * att);

			if (att > 0f) {
				CameraImpulse.shake(p.number("shakeIntensity", 1.0f) * att,
						Math.max(1, (int) p.number("shakeTicks", 12f)));
			}
		}

		/** Distance + line-of-sight falloff from the camera to the slam point. */
		private static float attenuation(Vec3 center) {
			Minecraft client = Minecraft.getInstance();
			ClientLevel level = client.level;
			LocalPlayer player = client.player;
			if (level == null || player == null) {
				return 0f;
			}
			Vec3 eye = client.gameRenderer.getMainCamera().getPosition();
			boolean los = level.clip(new ClipContext(eye, center,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
					.getType() == HitResult.Type.MISS;
			return ScreenFlash.attenuation(eye.distanceTo(center), 60.0, los);
		}

		@Override
		public void tick() {
			if (finished) {
				return;
			}
			age++;
			if (light != null) {
				float fade = Math.max(0f, 1f - (float) age / lightFadeTicks);
				if (fade <= 0f) {
					light.remove();
					light = null;
				} else {
					light.set(lightRgb, lightRadius * fade, lightBrightness * fade);
				}
			}
			ring.tick();
			if (ring.done() && light == null) {
				finished = true;
			}
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ring.render(ctx);
		}

		@Override
		public boolean done() {
			return finished;
		}

		@Override
		public void cancel() {
			finished = true;
			if (light != null) {
				light.remove();
				light = null;
			}
		}
	}

	/** Ends after one tick — the effect's real work happened in the factory. */
	private static final class DoneFx implements VfxEffect {
		private boolean done;

		@Override
		public void tick() {
			done = true;
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return done;
		}
	}
}
