package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.pandora.PandoraAbilities;
import io.github.grebeshok105.codex.hero.pandora.PandoraAttachments;
import io.github.grebeshok105.codex.core.model.AbilityAvailability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.hero.pandora.runtime.MirrorDimensionController;
import io.github.grebeshok105.codex.hero.pandora.runtime.VanityStrippedMobEffect;
import io.github.grebeshok105.codex.hero.pandora.runtime.PandoraDeathController;
import io.github.grebeshok105.codex.hero.pandora.runtime.SpatialBindController;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Wave I5c characterization: pins Pandora's behavior before her classes move into
 * {@code hero/pandora} / {@code client/hero/pandora}. Covers the roster contract
 * (persistent ability ids and slot order), transform passives (the 75% body scale),
 * Vanity Authority (per-tick caster immunity while the House is open, dimension-only
 * gating, the victim power strip), SpatialBind/SpaceCrush, and the death-controller
 * revival cinematic — health pinning, per-tick re-anchoring, the INVULNERABLE
 * control lock, killer-relative reappearance and the permanent
 * {@code pandora_revived} invulnerability afterwards. The client-side input lock is
 * not observable here; its server-visible correlate (frozen position + the lock
 * owners set) is what this suite asserts.
 */
public final class PandoraGameTests implements FabricGameTest {
	// Literal ids: constants move into the hero module when the wave lands.
	private static final ResourceLocation PANDORA = ModId.of("pandora");
	private static final ResourceLocation SCARAMOUCHE = ModId.of("scaramouche");
	private static final ResourceLocation WIND_PRISON = ModId.of("scaramouche_wind_prison");
	private static final ResourceLocation BODY_SCALE = ModId.of("modifiers/pandora/body_scale");

	private static Vec3 abs(GameTestHelper helper, double x, double y, double z) {
		return helper.absoluteVec(new Vec3(x, y, z));
	}

	/** Void world: players tick with gravity, so every spot they stand on needs a floor. */
	private static void floor(GameTestHelper helper, int x, int z) {
		helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	private static void park(GameTestHelper helper, ServerPlayer player, double x, double z) {
		Vec3 p = abs(helper, x, 1, z);
		player.teleportTo(p.x, p.y, p.z);
	}

	/**
	 * Far-away spot per test — the House pull radius is 50 while the harness lays
	 * structures ~40 blocks apart, so every House-opening test parks its players
	 * >100 blocks from every structure origin and every other slot (otherwise
	 * neighboring Houses absorb our actors non-deterministically). Players tick
	 * globally regardless of chunk state, so a far teleport is safe for them.
	 */
	private static Vec3 isoSpot(GameTestHelper helper, int slot) {
		Vec3 base = abs(helper, 4.5, 1, 4.5);
		return base.add(slot * 300.0, 0, slot * 300.0);
	}

	private static void floorAbs(GameTestHelper helper, Vec3 center) {
		BlockPos c = BlockPos.containing(center.x, center.y - 1, center.z);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.getLevel().setBlock(c.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	private static void parkIso(GameTestHelper helper, ServerPlayer player, Vec3 iso, double dx, double dz) {
		floorAbs(helper, iso);
		player.teleportTo(iso.x + dx, iso.y, iso.z + dz);
	}

	/** Polls once per game tick until {@code cond} holds or {@code tries} run out, then runs {@code body}. */
	private static void awaitTrue(GameTestHelper helper, BooleanSupplier cond, int tries, Runnable body) {
		if (tries <= 0 || cond.getAsBoolean()) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitTrue(helper, cond, tries - 1, body));
	}

	/** The hero is registered with its five abilities, in slot order. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void pandoraHeroExposesItsFiveAbilities(GameTestHelper helper) {
		Hero pandora = Heroes.get(PANDORA);
		helper.assertTrue(pandora != null, "the pandora hero is registered");
		List<ResourceLocation> ids = pandora.getAbilities();
		helper.assertTrue(ids.equals(List.of(
						PandoraAbilities.MIRROR_DIMENSION,
						PandoraAbilities.MIRROR_MODE_CYCLE,
						PandoraAbilities.SPATIAL_BIND,
						PandoraAbilities.SPACE_CRUSH,
						PandoraAbilities.VANITY_STRIP)),
				"ability list or slot order changed: " + ids);
		for (ResourceLocation id : ids) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " is not in AbilityRegistry");
		}
		helper.succeed();
	}

	/** Transform applies the -0.25 SCALE passive (75% body); untransform removes it and the hero. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void transformAppliesBodyScaleAndUntransformRestores(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "pd-body");
		floor(helper, 4, 4);
		park(helper, player, 4.5, 4.5);
		TestHeroes.transform(player, PANDORA);

		var scale = player.getAttribute(Attributes.SCALE);
		helper.assertTrue(scale != null && scale.getModifier(BODY_SCALE) != null,
				"transform installs the pandora body_scale modifier");
		helper.assertTrue(Math.abs(scale.getValue() - 0.75) < 0.001,
				"the passive shrinks Pandora to 75% scale");
		helper.assertTrue(player.getBbHeight() < 1.5f,
				"the shrunk hitbox is reflected in refreshed dimensions");

		HeroTransformService.forceUntransform(player);
		helper.assertFalse(HeroDataStore.get(player).hasHero(), "untransform clears the hero");
		helper.assertTrue(scale.getModifier(BODY_SCALE) == null,
				"untransform removes the body_scale modifier");
		helper.assertTrue(player.getBbHeight() > 1.7f, "the hitbox is restored");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/** The open House refreshes Vanity Authority on the caster every tick (immunity is
	 *  real — a hit is fully absorbed), unlocks her dimension-only abilities, and never
	 *  traps Pandora herself. Closing the House lets the immunity window fade. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void openHouseGrantsCasterAuthorityAndUnlocksAbilities(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "pd-house");
		parkIso(helper, pandora, isoSpot(helper, 1), 0.0, 0.0);
		TestHeroes.transform(pandora, PANDORA);
		Hero hero = Heroes.get(PANDORA);

		helper.assertTrue(hero.visibility(pandora, PandoraAbilities.SPATIAL_BIND)
						== AbilityAvailability.Visibility.HIDDEN
						&& !hero.canUseAbility(pandora, HeroDataStore.get(pandora), PandoraAbilities.SPATIAL_BIND),
				"dimension-only abilities are hidden and unusable without the House");

		AbilityRouter.activate(pandora, PandoraAbilities.MIRROR_DIMENSION);

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(MirrorDimensionController.hasActiveHouse(pandora), "the House is open");
			helper.assertFalse(MirrorDimensionController.isTrapped(pandora),
					"Pandora is never trapped by her own House (#6)");
			helper.assertTrue(pandora.hasEffect(MobEffects.DAMAGE_RESISTANCE)
							&& pandora.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 4
							&& pandora.hasEffect(MobEffects.FIRE_RESISTANCE),
					"Vanity Authority refreshes immunity on the caster each tick");
			helper.assertTrue(hero.visibility(pandora, PandoraAbilities.SPATIAL_BIND)
							== AbilityAvailability.Visibility.AVAILABLE
							&& hero.canUseAbility(pandora, HeroDataStore.get(pandora), PandoraAbilities.SPATIAL_BIND),
					"the open House unlocks the dimension-only abilities");

			TestPlayers.clearSpawnInvulnerability(pandora);
			pandora.hurt(helper.getLevel().damageSources().generic(), 5f);
			helper.assertTrue(pandora.getHealth() == pandora.getMaxHealth(),
					"level-5 resistance reduces the hit to zero damage");
			AbilityRouter.deactivate(pandora, PandoraAbilities.MIRROR_DIMENSION);
			helper.assertFalse(MirrorDimensionController.hasActiveHouse(pandora), "the House is closed");
			// Mock-player effect durations never tick down in this harness, so the
			// immunity-fade window cannot be observed — the closed House is the pin.
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** The strip toggle marks every trapped victim VANITY_STRIPPED immediately; while it
	 *  holds, the victim's own abilities are denied and uncharged. The 60-tick refresh
	 *  window means powers return on their own shortly after the toggle drops (simulated
	 *  via removeEffect — mock players never tick down durations in this harness). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void vanityStripSuppressesVictimAbilitiesInsideHouse(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "pd-vanity");
		Vec3 iso = isoSpot(helper, 2);
		parkIso(helper, pandora, iso, 0.0, 0.0);
		TestHeroes.transform(pandora, PANDORA);

		ServerPlayer victim = TestPlayers.join(helper, "vanity-victim");
		parkIso(helper, victim, iso, 2.0, 2.0);
		TestHeroes.transform(victim, SCARAMOUCHE);
		HeroDataStore.update(victim, d -> d.withResources(200f, d.mana()));

		AbilityRouter.activate(pandora, PandoraAbilities.MIRROR_DIMENSION);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(MirrorDimensionController.trappedVictims(pandora).contains(victim),
					"the initial absorb caught the victim");

			AbilityRouter.activate(pandora, PandoraAbilities.VANITY_STRIP);
			helper.assertTrue(HeroDataStore.get(pandora).isActive(PandoraAbilities.VANITY_STRIP),
					"the strip toggles on");
			helper.assertTrue(victim.hasEffect(VanityStrippedMobEffect.VANITY_STRIPPED),
					"activation strips every trapped victim immediately");

			float before = HeroDataStore.get(victim).energy();
			AbilityRouter.activate(victim, WIND_PRISON);
			helper.assertFalse(HeroDataStore.get(victim).isActive(WIND_PRISON),
					"a stripped victim cannot activate her own abilities");
			helper.assertTrue(HeroDataStore.get(victim).energy() == before, "nothing charged");

			AbilityRouter.activate(pandora, PandoraAbilities.VANITY_STRIP); // second press toggles off
			helper.assertFalse(HeroDataStore.get(pandora).isActive(PandoraAbilities.VANITY_STRIP),
					"the strip toggled off");
			// Mock players never tick down effect durations in this harness (the victim's
			// tickCount stays 0), so the 60t marker cannot decay naturally. Drop it the way
			// real expiry would, then check the powers return.
			victim.removeEffect(VanityStrippedMobEffect.VANITY_STRIPPED);
			AbilityRouter.activate(victim, WIND_PRISON);
			helper.assertTrue(HeroDataStore.get(victim).isActive(WIND_PRISON),
					"the victim's powers return after the strip");
			AbilityRouter.deactivate(victim, WIND_PRISON);
			AbilityRouter.deactivate(pandora, PandoraAbilities.MIRROR_DIMENSION);
			TestPlayers.leave(victim);
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** Inside the open House, SpatialBind ropes a trapped victim to its anchor with
	 *  crippling slows refreshed every tick and snaps back drift; SpaceCrush executes
	 *  every bound victim with the space_crush damage type and releases the bind. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void spatialBindRootsAndSpaceCrushKillsBoundVictim(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "pd-crush");
		Vec3 iso = isoSpot(helper, 3);
		parkIso(helper, pandora, iso, 0.0, 0.0);
		TestHeroes.transform(pandora, PANDORA);

		ServerPlayer victim = TestPlayers.join(helper, "bound-victim");
		parkIso(helper, victim, iso, 2.0, 2.0);
		TestPlayers.clearSpawnInvulnerability(victim);
		Vec3 anchor = victim.position();

		AbilityRouter.activate(pandora, PandoraAbilities.MIRROR_DIMENSION);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(MirrorDimensionController.trappedVictims(pandora).contains(victim),
					"the initial absorb caught the victim");
			AbilityRouter.activate(pandora, PandoraAbilities.SPATIAL_BIND);
			helper.assertTrue(SpatialBindController.isBound(victim),
					"the bind caught the trapped victim");
		});
		// The bind's effects land on the controller's next global tick, not on activate.
		helper.runAfterDelay(4, () -> {
			helper.assertTrue(victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
							&& victim.hasEffect(MobEffects.JUMP)
							&& victim.hasEffect(MobEffects.WEAKNESS),
					"the rope cripples refresh every tick");
			victim.teleportTo(anchor.x + 2.0, anchor.y, anchor.z);
		});
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(Math.abs(victim.getX() - anchor.x) < 0.5,
					"a bound victim is snapped back to the anchor");
			// The crush's damage source carries the pandora player, so it is gated by the
			// global server pvp flag — a concurrent test may hold it false; wrap and restore.
			boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
			helper.getLevel().getServer().setPvpAllowed(true);
			try {
				AbilityRouter.activate(pandora, PandoraAbilities.SPACE_CRUSH);
			} finally {
				helper.getLevel().getServer().setPvpAllowed(oldPvp);
			}
			helper.assertTrue(AbilityCooldowns.isOnCooldown(pandora, PandoraAbilities.SPACE_CRUSH),
					"the crush goes on cooldown after firing");
			helper.assertTrue(victim.isDeadOrDying(),
					"space_crush executes a bound victim");
			helper.assertFalse(SpatialBindController.isBound(victim), "the bind releases on crush");
			HeroTransformService.forceUntransform(pandora);
			helper.assertFalse(MirrorDimensionController.hasActiveHouse(pandora),
					"untransforming deactivates the House");
			helper.assertFalse(MirrorDimensionController.isTrapped(victim),
					"closing the House frees the victim's trap entry");
			TestPlayers.leave(victim);
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** A lethal hit never kills Pandora: it starts the 96-tick cinematic — health pinned,
	 *  the INVULNERABLE control lock held by herself, position re-anchored every tick —
	 *  then she reappears 1.4 blocks behind her killer, marked {@code pandora_revived}
	 *  and permanently untouchable. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void lethalHitTriggersRevivalCinematicBehindKiller(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "pd-revive");
		floor(helper, 4, 4);
		park(helper, pandora, 4.5, 4.5);
		TestHeroes.transform(pandora, PANDORA);
		TestPlayers.clearSpawnInvulnerability(pandora);
		Vec3 anchor = pandora.position();

		ServerPlayer killer = TestPlayers.join(helper, "pandora-killer");
		floor(helper, 2, 2);
		floor(helper, 2, 1); // she reappears 1.4 blocks behind (north of) the killer
		park(helper, killer, 2.5, 2.5);
		killer.setYRot(0f);

		// Player-attacker paths run under isPvpAllowed; the flag is global to the batch,
		// so wrap and restore it even though the hurt() path itself does not check pvp.
		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.getLevel().getServer().setPvpAllowed(true);
		try {
			helper.assertFalse(pandora.hurt(helper.getLevel().damageSources().playerAttack(killer), 1000f),
					"the lethal hit is cancelled by the cinematic trigger");
		} finally {
			helper.getLevel().getServer().setPvpAllowed(oldPvp);
		}
		// Latch: the poll predicate may not satisfy until the cinematic was observed once.
		AtomicBoolean seen = new AtomicBoolean();

		helper.runAfterDelay(10, () -> {
			helper.assertTrue(PandoraDeathController.isInCinematic(pandora), "the cinematic is running");
			seen.set(true);
			helper.assertTrue(pandora.getHealth() == pandora.getMaxHealth(), "health is pinned at max");
			helper.assertTrue(TestPlayers.lockOwners(pandora, ControlLockKind.INVULNERABLE)
							.contains(pandora.getUUID()),
					"Pandora holds her own INVULNERABLE lock");
			helper.assertTrue(pandora.isInvulnerable(), "the invulnerable flag is set");
			helper.assertFalse(pandora.hurt(helper.getLevel().damageSources().generic(), 5f),
					"mid-cinematic hits bounce off");
			// Server-visible lock: dragging her off the anchor is undone by the next tick.
			pandora.teleportTo(anchor.x, anchor.y, anchor.z - 4.0);
		});
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(pandora.distanceToSqr(anchor.x, anchor.y, anchor.z) < 0.5,
					"the cinematic re-anchors her every tick");
		});
		awaitTrue(helper, () -> seen.get() && !PandoraDeathController.isInCinematic(pandora), 150, () -> {
			helper.assertTrue(Boolean.TRUE.equals(pandora.getAttached(PandoraAttachments.PANDORA_REVIVED)),
					"the pandora_revived attachment persisted");
			helper.assertTrue(TestPlayers.lockOwners(pandora, ControlLockKind.INVULNERABLE)
							.contains(pandora.getUUID()),
					"the lock is re-acquired after revival");
			// yaw 0 → behind = -z; she lands 1.4 blocks north of the killer.
			helper.assertTrue(pandora.distanceToSqr(killer.getX(), killer.getY(), killer.getZ() - 1.4) < 1.0,
					"she reappears right behind the killer");
			helper.assertFalse(pandora.hurt(helper.getLevel().damageSources().generic(), 5f),
					"revived Pandora stays untouchable");
			helper.assertTrue(pandora.getHealth() == pandora.getMaxHealth(), "no damage taken");
			TestPlayers.leave(killer);
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** With no living killer the cinematic returns Pandora to the anchor; re-picking or
	 *  untransforming afterwards clears the revived flag, releases the lock and makes
	 *  her mortal again. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void revivalWithoutKillerReturnsToAnchorAndUntransformClearsIt(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "pd-anchor");
		floor(helper, 4, 4);
		park(helper, pandora, 4.5, 4.5);
		TestHeroes.transform(pandora, PANDORA);
		TestPlayers.clearSpawnInvulnerability(pandora);
		Vec3 anchor = pandora.position();

		helper.assertFalse(pandora.hurt(helper.getLevel().damageSources().generic(), 1000f),
				"lethal generic damage starts the cinematic");
		helper.assertTrue(PandoraDeathController.isInCinematic(pandora), "the cinematic is running");
		// Latch: the poll predicate may not satisfy until the cinematic was observed once.
		AtomicBoolean seen = new AtomicBoolean(true);

		awaitTrue(helper, () -> seen.get() && !PandoraDeathController.isInCinematic(pandora), 150, () -> {
			helper.assertTrue(pandora.distanceToSqr(anchor.x, anchor.y, anchor.z) < 0.5,
					"with no killer she reappears at the anchor");
			helper.assertTrue(Boolean.TRUE.equals(pandora.getAttached(PandoraAttachments.PANDORA_REVIVED)),
					"the revived flag persisted");

			HeroTransformService.forceUntransform(pandora);
			helper.assertTrue(pandora.getAttached(PandoraAttachments.PANDORA_REVIVED) == null,
					"hero-clear strips the revived flag");
			helper.assertTrue(TestPlayers.lockOwners(pandora, ControlLockKind.INVULNERABLE).isEmpty(),
					"hero-clear releases the invulnerability lock");
			helper.assertFalse(pandora.isInvulnerable(), "the flag restores with the lock");
			helper.assertTrue(pandora.hurt(helper.getLevel().damageSources().generic(), 5f),
					"untransformed Pandora is mortal again");
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}
}
