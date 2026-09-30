package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
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
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderPoseApi;
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

import java.util.List;

/**
 * Homelander's flight effects, tuned by {@code vfx/homelander/flight.json}:
 * <ul>
 *   <li>{@link #trail} — golden ribbons leaving each hand and foot while the
 *       tracked player is CRUISE/BOOST (self-terminates otherwise);</li>
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

	private static final ResourceLocation PARAMS = ModId.of("homelander/flight");
	private static final ResourceLocation BOOST_EMITTER = ModId.of("homelander_flight_boost");
	private static final ResourceLocation LANDING_EMITTER = ModId.of("homelander_flight_landing");
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Limb anchors in body space: (lateral, up, forward) per hand and foot. */
	private static final double[][] LIMB_OFFSETS = {
			{0.34, 1.35, 0.12}, {-0.34, 1.35, 0.12}, {0.12, 0.08, -0.05}, {-0.12, 0.08, -0.05}};

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

	/** Golden ribbons tracking the four limb anchors while CRUISE/BOOST. */
	private static final class TrailFx implements VfxEffect {
		private final @Nullable Entity entity;
		private final Vec3 fallback;
		private final List<TrailPattern> ribbons;
		private boolean finishing;

		private TrailFx(VfxSpawn spawn) {
			this.entity = spawn.source();
			this.fallback = spawn.origin();
			VfxParams p = params(spawn);
			int capacity = Math.max(4, (int) p.number("trailCapacity", 32f));
			float width = p.number("trailWidth", 0.05f);
			int color = p.color("trailColor", 0x90FFC538);
			int fade = Math.max(1, (int) p.number("trailFadeTicks", 8f));
			this.ribbons = List.of(
					new TrailPattern(capacity, width, color, fade),
					new TrailPattern(capacity, width, color, fade),
					new TrailPattern(capacity, width, color, fade),
					new TrailPattern(capacity, width, color, fade));
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
				FlightBodyTransform tilt = HomelanderPoseApi.currentBodyTransform(source.getId(), 1f).tilt();
				Vec3 feet = source.position();
				float bodyYaw = source instanceof net.minecraft.world.entity.LivingEntity living
						? living.yBodyRot : source.getYRot();
				for (int i = 0; i < LIMB_OFFSETS.length; i++) {
					double[] o = LIMB_OFFSETS[i];
					ribbons.get(i).push(HumanoidAnchors.tiltedPoint(
							feet, bodyYaw, o[0], o[1], o[2], tilt));
				}
			}
			ribbons.forEach(TrailPattern::tick);
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ribbons.forEach(ribbon -> ribbon.render(ctx));
		}

		@Override
		public boolean done() {
			return finishing && ribbons.stream().allMatch(TrailPattern::done);
		}

		@Override
		public void cancel() {
			ribbons.forEach(TrailPattern::cancel);
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
