package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.core.vfx.VfxShowcases;
import io.github.grebeshok105.codex.hero.homelander.runtime.IronFistsController;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.mechanic.ability.SharedAbilityIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Homelander's {@code /superheroes vfx scene} entries, registered from
 * {@code HomelanderModule} into the hero-agnostic {@link VfxShowcases}
 * registry. Positional scenes repeat {@code count} times on a ring around the
 * caller so a single command can build a heavy frame; ability-driving scenes
 * (flight, lasers) toggle the real activation path and ignore {@code count}.
 * Scenes that need the hero transform the caller to Homelander first —
 * a showcase should light up without a separate {@code /superheroes hero}.
 */
public final class HomelanderShowcases {
	static final ResourceLocation FLIGHT_PATH = ModId.of("homelander/flight_path");
	static final ResourceLocation LASERS = ModId.of("homelander/lasers");
	static final ResourceLocation LASERS_FLYING = ModId.of("homelander/lasers_flying");
	static final ResourceLocation SUN_DETONATION_SCENE = ModId.of("homelander/sun_detonation");
	static final ResourceLocation COMBAT = ModId.of("homelander/combat");

	private static final double RING_RADIUS = 6.0;
	private static final double CLAP_RANGE = 18.0;
	private static final double ROAR_RADIUS = 12.0;
	private static final double BROADCAST_RADIUS = 160.0;

	private HomelanderShowcases() {
	}

	public static void register() {
		VfxShowcases.register(FLIGHT_PATH, HomelanderShowcases::flightPath);
		VfxShowcases.register(LASERS, HomelanderShowcases::lasers);
		VfxShowcases.register(LASERS_FLYING, HomelanderShowcases::lasersFlying);
		VfxShowcases.register(SUN_DETONATION_SCENE, HomelanderShowcases::sunDetonation);
		VfxShowcases.register(COMBAT, HomelanderShowcases::combat);
	}

	/** Toggle the real flight path on the caller; a forward nudge makes the takeoff read. */
	private static void flightPath(ServerPlayer player, int count) {
		ensureHomelander(player);
		AbilityRouter.activate(player, SharedAbilityIds.FLIGHT);
		Vec3 look = player.getViewVector(1f).normalize();
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x + look.x * 0.8, motion.y + Math.max(look.y * 0.8, 0.4),
				motion.z + look.z * 0.8);
		player.hurtMarked = true;
	}

	/** Toggle the real eye-lasers activation — the channel stream observers must see. */
	private static void lasers(ServerPlayer player, int count) {
		ensureHomelander(player);
		AbilityRouter.activate(player, HomelanderAbilityIds.EYE_LASERS);
	}

	/** Flight plus lasers — the "lasers leave the flying player's eyes" showcase. */
	private static void lasersFlying(ServerPlayer player, int count) {
		ensureHomelander(player);
		AbilityRouter.activate(player, SharedAbilityIds.FLIGHT);
		AbilityRouter.activate(player, HomelanderAbilityIds.EYE_LASERS);
		Vec3 look = player.getViewVector(1f).normalize();
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x + look.x * 0.8, motion.y + Math.max(look.y * 0.8, 0.4),
				motion.z + look.z * 0.8);
		player.hurtMarked = true;
	}

	/** Visual-only: the detonation event with none of the gameplay blast it normally accompanies. */
	private static void sunDetonation(ServerPlayer player, int count) {
		Vec3 center = player.position().add(0.0, 1.0, 0.0);
		for (int i = 0; i < Math.max(1, count); i++) {
			Vec3 pos = ringPoint(center, count, i, count > 1 ? RING_RADIUS : 0.0);
			VfxFx.eventAround(player.serverLevel(), HomelanderVfxIds.SUN_DETONATION,
					pos, pos, 1f, BROADCAST_RADIUS);
		}
	}

	/** Iron Fists hit + Clap + Roar at {@code count} dummy positions ringing the caller. */
	private static void combat(ServerPlayer player, int count) {
		ServerLevel level = player.serverLevel();
		Vec3 center = player.position();
		Vec3 forward = player.getViewVector(1f).normalize();
		for (int i = 0; i < Math.max(1, count); i++) {
			Vec3 dummy = ringPoint(center, count, i, count > 1 ? RING_RADIUS : 0.0);
			Vec3 chest = dummy.add(0.0, 1.0, 0.0);
			VfxFx.eventAround(level, HomelanderVfxIds.IRON_FISTS_HIT, chest, dummy,
					(float) IronFistsController.SHOCKWAVE_RADIUS, BROADCAST_RADIUS);
			Vec3 hands = dummy.add(0.0, 1.5, 0.0);
			VfxFx.eventAround(level, HomelanderVfxIds.CLAP, hands,
					hands.add(forward.scale(CLAP_RANGE)), 1f, BROADCAST_RADIUS);
			Vec3 mouth = dummy.add(0.0, 1.6, 0.0).add(forward.scale(0.6));
			VfxFx.eventAround(level, HomelanderVfxIds.ROAR, mouth,
					mouth.add(forward.scale(ROAR_RADIUS)), 1f, BROADCAST_RADIUS);
		}
	}

	private static Vec3 ringPoint(Vec3 center, int count, int index, double radius) {
		if (count <= 1 || radius <= 0.0) {
			return center;
		}
		double angle = (Math.PI * 2.0) * index / count;
		return center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
	}

	private static void ensureHomelander(ServerPlayer player) {
		HeroData data = HeroDataStore.get(player);
		if (data.hasHero() && HomelanderHero.ID.equals(data.heroId())) {
			return;
		}
		// Best effort — a transform cooldown or a missing hero just leaves the
		// ability activation to no-op through the router's normal gates.
		HeroTransformService.transform(player, HomelanderHero.ID);
	}
}
