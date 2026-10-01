package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.hero.regulus.emf.RegulusEmf;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Regulus's Visual Core registration hub — every {@code RegulusVfxIds}
 * one-shot and channel factory is wired here. The {@code regulus/anim/*}
 * events primarily start the authored EMF clip on the source player
 * ({@link RegulusEmf}); the impact/flash rows of the design §6 table add
 * backend emit/light/flash/distortion layers on top. Tuning lives in
 * {@code assets/superheroes/vfx/regulus/<effect>.json}.
 */
public final class RegulusFx {
	private static final int WHITE = 0xFFFFFF;
	private static final int GOLD = 0xFFC800;
	private static final int DIM_GOLD = 0x8C6400;

	private RegulusFx() {
	}

	public static void register(HeroClientContext ctx) {
		ctx.vfx(RegulusVfxIds.ANIM_GREEDS_EMBRACE_CAST, RegulusFx::embraceCast);
		ctx.vfx(RegulusVfxIds.ANIM_LION_HEART_ACTIVATION, RegulusFx::lionHeartActivation);
		ctx.vfx(RegulusVfxIds.ANIM_EVANGELIUM_ACTIVATION, RegulusFx::evangeliumActivation);
		ctx.vfx(RegulusVfxIds.ANIM_EVANGELIUM_DEACTIVATION, RegulusFx::evangeliumDeactivation);
		ctx.vfx(RegulusVfxIds.ANIM_DEBRIS_KICK, RegulusFx::debrisKick);
		ctx.vfx(RegulusVfxIds.ANIM_MANIA_OF_GREED_CAST, RegulusFx::maniaCast);
		ctx.vfx(RegulusVfxIds.ANIM_COUNTER_ATTACK, RegulusFx::counterAttack);
		ctx.vfx(RegulusVfxIds.ANIM_LION_ROAR, RegulusFx::lionRoar);
		ctx.vfx(RegulusVfxIds.EVANGELIUM_MAJOR, RegulusFx::evangeliumMajor);
		ctx.vfx(RegulusVfxIds.DEBRIS_IMPACT, RegulusFx::debrisImpact);
		ctx.vfx(RegulusVfxIds.COUNTER_LIFT, RegulusFx::counterLift);
		ctx.vfx(RegulusVfxIds.COUNTER_SLAM, RegulusFx::counterSlam);
		ctx.vfx(RegulusVfxIds.LANDING, RegulusFx::landing);
		ctx.vfxChannel(RegulusVfxIds.CHANNEL_GREED_MAGNET, GreedMagnetChannel::new);
		ctx.vfxChannel(RegulusVfxIds.CHANNEL_GREED_STASIS, StasisDomeChannel::new);
		ctx.vfxChannel(RegulusVfxIds.CHANNEL_LION_HEART_DOME, LionHeartDomeChannel::new);
		ctx.vfxChannel(RegulusVfxIds.CHANNEL_HEART_PULSE, HeartPulseChannel::new);
	}

	/** Greed's Embrace windup — clip start only; the dome visual is the channel. */
	private static VfxEffect embraceCast(VfxSpawn spawn) {
		RegulusEmf.embraceCastStarted(spawn.source());
		return new DoneFx();
	}

	/**
	 * Lion-heart cast: clip start plus a short cold light pulse on the
	 * player (the dome itself opens through the channel when protection
	 * engages at the authored 0.70 s trigger).
	 */
	private static VfxEffect lionHeartActivation(VfxSpawn spawn) {
		RegulusEmf.lionHeartActivationStarted(spawn.source());
		Entity source = spawn.source();
		if (source == null) {
			return new DoneFx();
		}
		Vec3 chest = source.position().add(0, source.getBbHeight() * 0.6, 0);
		return new TimedLightFx(chest, 0xCFE8FF, 5f, 0.5f, 24);
	}

	/**
	 * Ritual begin: clip start, the golden glow under the floating book
	 * ({@code regulus_evangelium_gold} emitter + a warm light for the
	 * 68-tick activation window).
	 */
	private static VfxEffect evangeliumActivation(VfxSpawn spawn) {
		RegulusEmf.evangeliumActivationStarted(spawn.source());
		Vec3 origin = spawn.origin();
		VfxBackend backend = VfxBackends.current();
		backend.emit(ModId.of("regulus_evangelium_gold"), origin.add(0, 1.4, 0));
		backend.flash(0.25f, DIM_GOLD);
		return new TimedLightFx(origin.add(0, 1.6, 0), GOLD, 4f, 0.45f, 68);
	}

	/** Madness collapse: clip start, a drained dim flash. */
	private static VfxEffect evangeliumDeactivation(VfxSpawn spawn) {
		RegulusEmf.evangeliumDeactivationStarted(spawn.source());
		VfxBackends.current().flash(0.18f, 0x603030);
		return new DoneFx();
	}

	/** Authored 1.66 s major frame: gold-white flash + burst + distortion spike. */
	private static VfxEffect evangeliumMajor(VfxSpawn spawn) {
		Vec3 origin = spawn.origin();
		VfxBackend backend = VfxBackends.current();
		backend.emit(ModId.of("regulus_evangelium_gold"), origin.add(0, 1.5, 0));
		backend.flash(0.55f, 0xFFE8A0);
		backend.distortion(origin, 2.2f, 0.5f);
		return new TimedLightFx(origin.add(0, 1.6, 0), GOLD, 6f, 0.8f, 18);
	}

	/** Debris kick start: clip + the rising-shards emitter under the player. */
	private static VfxEffect debrisKick(VfxSpawn spawn) {
		RegulusEmf.debrisKickStarted(spawn.source());
		VfxBackends.current().emit(ModId.of("regulus_debris_lift"), spawn.origin());
		return new DoneFx();
	}

	/** The authored 0.70 s impact frame: dust cone, shard burst, distortion. */
	private static VfxEffect debrisImpact(VfxSpawn spawn) {
		Vec3 origin = spawn.origin();
		VfxBackend backend = VfxBackends.current();
		backend.emit(ModId.of("regulus_debris_impact"), origin);
		backend.distortion(origin, 2.6f, 0.65f);
		backend.flash(0.15f, 0xD8C8A0);
		return new TimedLightFx(origin.add(0, 0.4, 0), DIM_GOLD, 3.5f, 0.35f, 8);
	}

	/** Mania-of-greed cast clip; the magnet thread itself is the channel. */
	private static VfxEffect maniaCast(VfxSpawn spawn) {
		RegulusEmf.maniaCastStarted(spawn.source());
		Vec3 origin = spawn.origin();
		VfxBackend backend = VfxBackends.current();
		backend.emit(ModId.of("regulus_evangelium_gold"), origin.add(0, 1.2, 0));
		return new DoneFx();
	}

	/** Counter-attack clip — ARRIVE→SLAM transition. */
	private static VfxEffect counterAttack(VfxSpawn spawn) {
		RegulusEmf.counterAttackStarted(spawn.source());
		return new DoneFx();
	}

	/** Reserve clip start — currently nothing emits the event. */
	private static VfxEffect lionRoar(VfxSpawn spawn) {
		RegulusEmf.lionRoarStarted(spawn.source());
		return new DoneFx();
	}

	/** Counter lift: white screen flash at the freeze moment. */
	private static VfxEffect counterLift(VfxSpawn spawn) {
		VfxBackends.current().flash(0.55f, WHITE);
		return new DoneFx();
	}

	/** Counter slam: speed-line burst + distortion at the impact point. */
	private static VfxEffect counterSlam(VfxSpawn spawn) {
		Vec3 origin = spawn.origin();
		VfxBackend backend = VfxBackends.current();
		backend.emit(ModId.of("regulus_slam_lines"), origin);
		backend.distortion(origin, 1.8f, 0.8f);
		backend.flash(0.3f, WHITE);
		return new DoneFx();
	}

	/** Hard landing: dust shock emitter + distortion scaled by the fall tier. */
	private static VfxEffect landing(VfxSpawn spawn) {
		Vec3 origin = spawn.origin();
		float scale = Math.max(0.5f, spawn.scale());
		VfxBackend backend = VfxBackends.current();
		backend.emit(ModId.of("regulus_landing_shock"), origin);
		backend.distortion(origin, 1.4f * scale, 0.5f * scale);
		return new DoneFx();
	}

	/**
	 * A backend light held for {@code ticks} at a fixed position — used where
	 * a one-shot event leaves a lingering glow (ritual book, impact).
	 */
	private static final class TimedLightFx implements VfxEffect {
		private final int rgb;
		private final float radius;
		private final float brightness;
		private final int lifeTicks;
		private @Nullable LightHandle light;
		private int age;
		private boolean done;

		TimedLightFx(Vec3 pos, int rgb, float radius, float brightness, int lifeTicks) {
			this.rgb = rgb;
			this.radius = radius;
			this.brightness = brightness;
			this.lifeTicks = lifeTicks;
			this.light = VfxBackends.current().light(pos, rgb & 0xFFFFFF, radius, brightness);
		}

		@Override
		public void tick() {
			age++;
			if (age >= lifeTicks) {
				done = true;
				removeLight();
				return;
			}
			// Fade out across the last 8 ticks.
			LightHandle handle = light;
			if (handle != null && age >= lifeTicks - 8) {
				float left = (lifeTicks - age) / 8f;
				handle.set(rgb & 0xFFFFFF, radius, brightness * left);
			}
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return done;
		}

		@Override
		public void cancel() {
			removeLight();
		}

		private void removeLight() {
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
