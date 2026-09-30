package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.client.core.flight.FlightPresentation;
import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderEmf;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Homelander's Visual Core registration hub — every {@code HomelanderVfxIds}
 * effect/channel factory and the flight presentation get wired here. Task 8
 * registers the LANDING event plus the flight trail/boost effects and the
 * {@link FlightPresentation}; Tasks 9–12 extend {@link #register} with their
 * ids (laser channel, sun, iron fists, clap, roar, milk).
 */
public final class HomelanderFx {
	private HomelanderFx() {
	}

	public static void register(HeroClientContext ctx) {
		ctx.vfx(HomelanderVfxIds.LANDING, FlightFx::landing);
		ctx.vfx(FlightFx.TRAIL, FlightFx::trail);
		ctx.vfx(FlightFx.BOOST, FlightFx::boost);
		ctx.vfxChannel(HomelanderVfxIds.LASER, EyeLaserChannel::new);
		ctx.vfx(HomelanderVfxIds.SUN_CHARGE, SunChargeFx::create);
		ctx.vfx(HomelanderVfxIds.SUN_DETONATION, SunDetonationFx::create);
		ctx.vfx(HomelanderVfxIds.MADNESS_CRASH, SunDetonationFx::create);
		ctx.vfx(HomelanderVfxIds.MILK_DRINK, HomelanderFx::milkDrink);
		ctx.vfx(HomelanderVfxIds.IRON_FISTS_ON, IronFistsFx::activate);
		ctx.vfx(HomelanderVfxIds.IRON_FISTS_OFF, IronFistsFx::deactivate);
		ctx.vfx(HomelanderVfxIds.IRON_FISTS_HIT, IronFistsFx::hit);
		ctx.vfx(HomelanderVfxIds.CLAP, HomelanderFx::clapStart);
		ctx.vfx(HomelanderVfxIds.CLAP_IMPACT, ClapFx::create);
		ctx.vfx(HomelanderVfxIds.CLAP_CANCEL, HomelanderFx::clapCancel);
		ctx.vfx(HomelanderVfxIds.ROAR, RoarFx::create);
		// No player clips: the EMF jem presents Homelander's model. The
		// presentation stays registered for trail/boost effects and sounds;
		// poseParams layers the authored emfBoost* keys over flight/pose for
		// EMF-owned players (§7 stage 4).
		ctx.flightPresentation(new FlightPresentation(
				null, null, null, null, null,
				FlightFx.TRAIL,
				FlightFx.BOOST,
				HomelanderSounds.FLIGHT_LOOP,
				HomelanderSounds.FLIGHT_TAKEOFF,
				HomelanderSounds.FLIGHT_BOOST,
				HomelanderSounds.FLIGHT_LAND,
				FlightFx.PARAMS));
	}

	/** {@code CLAP} one-shot — starts the authored clip via the EMF pose state. */
	private static VfxEffect clapStart(VfxSpawn spawn) {
		Entity source = spawn.source();
		if (source != null) {
			HomelanderEmf.clapStarted(source);
		}
		return new DoneFx();
	}

	/** {@code CLAP_CANCEL} one-shot — the server dropped the pending hit. */
	private static VfxEffect clapCancel(VfxSpawn spawn) {
		Entity source = spawn.source();
		if (source != null) {
			HomelanderEmf.clapCancelled(source);
		}
		return new DoneFx();
	}

	/**
	 * MILK_DRINK one-shot — the bound {@code homelander.milk.drink} sound on
	 * the drinking player. Hosted here in the hub rather than as its own
	 * class: no patterns, no state.
	 */
	private static VfxEffect milkDrink(VfxSpawn spawn) {
		Entity source = spawn.source();
		if (source != null) {
			Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(
					HomelanderSounds.MILK_DRINK, SoundSource.PLAYERS, 1f, 1f,
					source, source.getRandom().nextLong()));
		} else {
			Vec3 pos = spawn.origin();
			Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
					HomelanderSounds.MILK_DRINK, SoundSource.PLAYERS, 1f, 1f,
					RandomSource.create(), pos.x, pos.y, pos.z));
		}
		return new DoneFx();
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
