package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import io.github.grebeshok105.codex.client.core.flight.FlightPoseTracker;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.anchor.HumanoidAnchors;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ImpactPattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ShockwavePattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.TrailPattern;
import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderPoseState;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Homelander's flight effects, tuned by {@code vfx/homelander/flight.json}:
 * <ul>
 *   <li>{@link #trail} — supersonic signature while the tracked player is
 *       CRUISE/BOOST: a soft vapor contrail trailing the body (core +
 *       wide faint sheath), thin speed streaks off the fists, and pressure
 *       rings popping perpendicular to the smoothed flight direction once
 *       the speed stays supersonic — the look of punching through the air
 *       barrier (self-terminates otherwise);</li>
 *   <li>{@link #boost} — emitter burst + shock ring on BOOST entry;</li>
 *   <li>{@link #landing} — impact composite for the {@code LANDING} event:
 *       dust emitter + distortion through {@link ImpactPattern}, an expanding
 *       {@link ShockwavePattern}, and {@code homelander.flight.land} — the
 *       single landing sound.</li>
 * </ul>
 */
public final class FlightFx {
	static final ResourceLocation TRAIL = ModId.of("homelander/flight_trail");
	static final ResourceLocation BOOST = ModId.of("homelander/flight_boost");

	static final ResourceLocation PARAMS = ModId.of("homelander/flight");
	private static final ResourceLocation BOOST_EMITTER = ModId.of("homelander_flight_boost");
	private static final ResourceLocation LANDING_EMITTER = ModId.of("homelander_flight_landing");
	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final ResourceLocation SOFT_TRAIL_TEXTURE =
			ModId.of("textures/effect/soft_trail.png");

	/** Fist anchors in body space: (lateral, up, forward). */
	private static final double[][] FIST_OFFSETS = {
			{0.34, 1.35, 0.12}, {-0.34, 1.35, 0.12}};
	/** Chest anchor where the vapor cone and pressure rings attach. */
	private static final double[] CHEST_OFFSET = {0.0, 1.35, 0.0};

	private FlightFx() {
	}

	public static VfxEffect trail(VfxSpawn spawn) {
		return new TrailFx(spawn);
	}

	public static VfxEffect boost(VfxSpawn spawn) {
		return new BoostFx(spawn);
	}

	public static VfxEffect landing(VfxSpawn spawn) {
		return new LandingFx(spawn);
	}

	private static VfxParams params(VfxSpawn spawn) {
		// All Homelander flight tuning lives in homelander/flight.json; the
		// effect-id params file may not exist, so merge spawn params as fallback.
		VfxParams p = VfxParamsLoader.get(PARAMS);
		return p == VfxParams.EMPTY ? spawn.params() : p;
	}

	/**
	 * Supersonic signature: a soft vapor contrail off the chest (dense core +
	 * wide translucent sheath), hairline speed streaks off the fists, and a
	 * pressure ring popped every {@code ringIntervalTicks} perpendicular to
	 * the smoothed flight direction once the speed has held at or above
	 * {@code ringSpeed} for {@link #RING_SUSTAIN_TICKS} consecutive ticks —
	 * the air-barrier break left hanging behind.
	 */
	private static final class TrailFx implements VfxEffect {
		private static final int CONE_CORE = 0;
		private static final int CONE_SHEATH = 1;
		private static final int STREAK_L = 2;
		private static final int STREAK_R = 3;
		/** Ticks the smoothed speed must stay at {@code ringSpeed} before rings emit. */
		private static final int RING_SUSTAIN_TICKS = 5;

		private final @Nullable Entity entity;
		private final VfxParams params;
		private final List<TrailPattern> ribbons;
		private final List<ShockwavePattern> rings = new ArrayList<>();
		private final int ringInterval;
		private final float ringSpeed;
		private int ringClock;
		private int fastTicks;
		private boolean finishing;

		private TrailFx(VfxSpawn spawn) {
			this.entity = spawn.source();
			this.params = params(spawn);
			int capacity = Math.max(4, (int) params.number("trailCapacity", 48f));
			int fade = Math.max(1, (int) params.number("trailFadeTicks", 10f));
			this.ribbons = List.of(
					TrailPattern.soft(capacity, params.number("coneCoreWidth", 0.16f),
							params.color("coneCoreColor", 0x5AF4FAFF), fade, SOFT_TRAIL_TEXTURE),
					TrailPattern.soft(capacity, params.number("coneSheathWidth", 0.5f),
							params.color("coneSheathColor", 0x28D9F2FF), fade, SOFT_TRAIL_TEXTURE),
					TrailPattern.soft(capacity, params.number("streakWidth", 0.03f),
							params.color("streakColor", 0x4DFFFFFF), fade, SOFT_TRAIL_TEXTURE),
					TrailPattern.soft(capacity, params.number("streakWidth", 0.03f),
							params.color("streakColor", 0x4DFFFFFF), fade, SOFT_TRAIL_TEXTURE));
			this.ringInterval = Math.max(1, (int) params.number("ringIntervalTicks", 12f));
			this.ringSpeed = params.number("ringSpeed", 1.0f);
		}

		@Override
		public void tick() {
			Entity source = entity;
			ClientFlightState.State state = source != null
					? ClientFlightState.get(source.getId()) : null;
			boolean active = source != null && !source.isRemoved() && state != null
					&& (state.phase() == FlightPhase.CRUISE || state.phase() == FlightPhase.BOOST);
			if (!active) {
				finishing = true;
				ribbons.forEach(TrailPattern::finish);
			} else {
				FlightBodyTransform tilt = FlightPoseTracker.transform(source.getId(), 1f);
				Vec3 feet = source.position();
				float bodyYaw = source instanceof net.minecraft.world.entity.LivingEntity living
						? living.yBodyRot : source.getYRot();
				Vec3 chest = HumanoidAnchors.tiltedPoint(
						feet, bodyYaw, CHEST_OFFSET[0], CHEST_OFFSET[1], CHEST_OFFSET[2], tilt);
				ribbons.get(CONE_CORE).push(chest);
				ribbons.get(CONE_SHEATH).push(chest);
				for (int i = 0; i < FIST_OFFSETS.length; i++) {
					double[] o = FIST_OFFSETS[i];
					ribbons.get(STREAK_L + i).push(HumanoidAnchors.tiltedPoint(
							feet, bodyYaw, o[0], o[1], o[2], tilt));
				}
				Vec3 velocity = flightVelocity(source);
				if (velocity.length() >= ringSpeed) {
					fastTicks++;
				} else {
					fastTicks = 0;
				}
				if (++ringClock % ringInterval == 0 && fastTicks >= RING_SUSTAIN_TICKS) {
					rings.add(new ShockwavePattern(chest,
							params.number("ringRadius", 1.4f),
							Math.max(1, (int) params.number("ringTicks", 9f)),
							params.color("ringColor", 0x40E8FAFF),
							params.number("ringBand", 0.22f),
							velocity));
				}
			}
			ribbons.forEach(TrailPattern::tick);
			rings.removeIf(ring -> {
				ring.tick();
				return ring.done();
			});
		}

		/**
		 * Velocity driving the ring gate and ring plane: the pose state's
		 * smoothed render velocity (the same source the EMF pose consumes —
		 * tracked for remote players too) when available; otherwise the
		 * entity's raw delta movement. A ring only emits above
		 * {@code ringSpeed}, so this vector is never degenerate at emission.
		 */
		private static Vec3 flightVelocity(Entity source) {
			Vec3 smoothed = HomelanderPoseState.smoothedVelocity(source.getUUID());
			if (smoothed != null) {
				return smoothed;
			}
			Vec3 motion = source.getDeltaMovement();
			return motion.lengthSqr() > 1e-4 ? motion : Vec3.ZERO;
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ribbons.forEach(ribbon -> ribbon.render(ctx));
			rings.forEach(ring -> ring.render(ctx));
		}

		@Override
		public boolean done() {
			return finishing && ribbons.stream().allMatch(TrailPattern::done) && rings.isEmpty();
		}

		@Override
		public void cancel() {
			ribbons.forEach(TrailPattern::cancel);
			rings.clear();
		}
	}

	/** BOOST-entry burst: quasar emitter + expanding shock ring. */
	private static final class BoostFx implements VfxEffect {
		private final Vec3 center;
		private final ShockwavePattern ring;
		private final int emitTicks;
		private int age;

		private BoostFx(VfxSpawn spawn) {
			this.center = spawn.origin();
			VfxParams p = params(spawn);
			float radius = p.number("boostRingRadius", 2.5f) * Math.max(0.1f, spawn.scale());
			this.ring = new ShockwavePattern(center, radius,
					Math.max(1, (int) p.number("boostRingTicks", 10f)),
					p.color("boostRingColor", 0x60FFC538), p.number("boostBand", 0.2f));
			this.emitTicks = Math.max(1, (int) p.number("boostEmitTicks", 3f));
		}

		@Override
		public void tick() {
			age++;
			if (age <= emitTicks) {
				VfxBackends.current().emit(BOOST_EMITTER, center);
			}
			ring.tick();
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ring.render(ctx);
		}

		@Override
		public boolean done() {
			return age > emitTicks && ring.done();
		}
	}

	/** LANDING event composite: emitter + distortion + shock ring + land sound. */
	private static final class LandingFx implements VfxEffect {
		private final ShockwavePattern ring;

		private LandingFx(VfxSpawn spawn) {
			Vec3 center = spawn.origin();
			VfxParams p = params(spawn);
			float scale = Math.max(0.1f, spawn.scale());
			this.ring = new ShockwavePattern(center,
					p.number("landingRingRadius", 5f) * scale,
					Math.max(1, (int) p.number("landingRingTicks", 14f)),
					p.color("landingRingColor", 0x70FFE07A), p.number("landingBand", 0.18f));
			ImpactPattern.spawn(VfxBackends.current(), center, UP, LANDING_EMITTER, p);
			float volume = Mth.clamp(
					p.number("landBaseVolume", 0.8f) + scale * p.number("landVolumeScale", 0.5f),
					0f, 4f);
			Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
					HomelanderSounds.FLIGHT_LAND, SoundSource.PLAYERS, volume,
					p.number("landPitch", 1f), RandomSource.create(),
					center.x, center.y, center.z));
		}

		@Override
		public void tick() {
			ring.tick();
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ring.render(ctx);
		}

		@Override
		public boolean done() {
			return ring.done();
		}
	}
}
