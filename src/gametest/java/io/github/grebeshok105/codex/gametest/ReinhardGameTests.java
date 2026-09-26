package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.damage.ModDamageTypes;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardAbilities;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardAttachments;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardHero;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardItems;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardWishAbility;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pins for the G1 Reinhard module move: written against the pre-move layout and must
 * pass identically after {@code hero/reinhard/} absorbs the server side.
 *
 * <p>Sword gate: {@code AbilityIds.isReinhardSwordOnly} was dead code — a repo-wide
 * call-site search found no caller (not the router, not the abilities, not HUD
 * visibility), so the four "sword-only" abilities activate with empty hands: the
 * energy is charged and the effects run. The only sword-related refusal in the
 * activation path is {@code reinhard_sword_draw} itself: without the damage gate's
 * ready flag its {@code tryActivate} shows {@code ability.superheroes.reinhard_sword_draw.no_worthy}
 * and returns {@code false} — for the router a silent refusal (no toggle, no charge).
 */
public final class ReinhardGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void swordOnlyAbilitiesRequireTheSword(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		helper.assertTrue(TestPlayers.count(player, ReinhardItems.ROYAL_ICICLE) == 0,
				"fresh Reinhard holds no Royal Icicle");
		float energy0 = HeroDataStore.get(player).energy();

		AbilityRouter.activate(player, ReinhardAbilities.REINHARD_AIR_SLASH);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ReinhardAbilities.REINHARD_AIR_SLASH),
				"no gate stops Air Slash without the sword");
		helper.assertTrue(HeroDataStore.get(player).energy() == energy0 - 200f,
				"Air Slash charged 200 energy");

		AbilityRouter.activate(player, ReinhardAbilities.REINHARD_SWORD_WAVE);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ReinhardAbilities.REINHARD_SWORD_WAVE),
				"no gate stops Sword Wave without the sword");
		helper.assertTrue(HeroDataStore.get(player).energy() == energy0 - 450f,
				"Sword Wave charged another 250");

		AbilityRouter.activate(player, ReinhardAbilities.REINHARD_COUNTER_RIPOSTE);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ReinhardAbilities.REINHARD_COUNTER_RIPOSTE),
				"no gate stops Counter Riposte without the sword");
		helper.assertTrue(player.getAttachedOrCreate(ReinhardAttachments.STATE).riposteExpireTick()
						> player.serverLevel().getGameTime(),
				"the riposte window armed");

		AbilityRouter.activate(player, ReinhardAbilities.REINHARD_DIVINE_AURA);
		helper.assertTrue(HeroDataStore.get(player).isActive(ReinhardAbilities.REINHARD_DIVINE_AURA),
				"no gate stops Divine Aura without the sword");

		// The only sword-side denial that exists: the draw itself is refused until
		// ReinhardSwordDrawGateController marks him ready (30 damage from one attacker
		// inside 30 s). tryActivate fails with the no_worthy message and no sword is issued.
		AbilityRouter.activate(player, ReinhardAbilities.REINHARD_SWORD_DRAW);
		helper.assertFalse(HeroDataStore.get(player).isActive(ReinhardAbilities.REINHARD_SWORD_DRAW),
				"the draw is refused while the damage gate is not ready");
		helper.assertFalse(player.getAttachedOrCreate(ReinhardAttachments.STATE).swordDrawn(),
				"swordDrawn stays false");
		helper.assertTrue(TestPlayers.count(player, ReinhardItems.ROYAL_ICICLE) == 0,
				"no Royal Icicle was issued");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * {@code ReinhardAttachments.STATE} is persistent + copyOnDeath: fields the hero-clear
	 * does not reset (spent wishes, the judgment mark) stay on the player through death,
	 * while {@code clearAdaptations} — fired by the death-triggered untransform — drops the
	 * adaptations and the drawn flag. Mock players have no real respawn round-trip, so the
	 * respawn half is driven through the same hook the dispatcher fires (convention from
	 * {@code LifecycleGameTests.doomsdayKeepsHeroOnDeath}).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stateSurvivesDeath(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		UUID mark = UUID.randomUUID();
		player.setAttached(ReinhardAttachments.STATE,
				player.getAttachedOrCreate(ReinhardAttachments.STATE)
						.withWishesUsed(2)
						.withJudgmentTarget(Optional.of(mark), 4242L)
						.withAdaptedDamageTypes(List.of("minecraft:magic"))
						.withSwordDrawn(true)
						// Phoenix spent, otherwise Second Coming intercepts the kill.
						.withPhoenixUsed(true));

		player.kill();
		helper.assertFalse(player.isAlive(), "phoenix spent — the kill is real");
		HeroTransformService.onPlayerRespawn(player);

		ReinhardState after = player.getAttachedOrCreate(ReinhardAttachments.STATE);
		helper.assertTrue(after.wishesUsed() == 2,
				"spent wishes persist through death");
		helper.assertTrue(after.judgmentTarget().isPresent()
						&& mark.equals(after.judgmentTarget().get()),
				"the judgment mark target persists through death");
		helper.assertTrue(after.adaptedDamageTypes().isEmpty(),
				"clearAdaptations ran — the attachment was not merely untouched");
		helper.assertFalse(after.swordDrawn(),
				"the drawn flag is reset by the death untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * {@code #superheroes:beam} holds exactly the six damage types the pre-move
	 * {@code ReinhardController} key list had — no key dropped, none added.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void beamTagMatchesLegacyList(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		HolderSet.Named<DamageType> tag = player.serverLevel().registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.get(ModDamageTypes.BEAM)
				.orElseThrow(() -> new IllegalStateException("#superheroes:beam tag missing"));
		Set<String> ids = tag.stream()
				.map(Holder::unwrapKey)
				.filter(Optional::isPresent)
				.map(k -> k.get().location().toString())
				.collect(Collectors.toSet());
		Set<String> expected = Set.of(
				"superheroes:eye_laser",
				"superheroes:repulsor",
				"superheroes:unibeam",
				"superheroes:homelander_eye_laser",
				"superheroes:homelander_heat_vision",
				"superheroes:goku_kamehameha");
		helper.assertTrue(expected.equals(ids),
				"#superheroes:beam must equal the legacy six-key list, got " + ids);
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * {@link ReinhardWishAbility#handleWishConfirm} — first {@code C2SGuards.requireHero}
	 * consumer: a confirm from a player who is not Reinhard is a no-op; the same call on a
	 * real Reinhard still applies the wish (wishesUsed +1, damage type adapted).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wishConfirmNoopsForOtherHero(GameTestHelper helper) {
		ServerPlayer stranger = TestPlayers.join(helper);
		// no transform — definitely not Reinhard
		stranger.setAttached(ReinhardAttachments.STATE,
				stranger.getAttachedOrCreate(ReinhardAttachments.STATE)
						.withRecentDamageTypes(List.of("minecraft:magic")));
		ReinhardWishAbility.handleWishConfirm(stranger, "minecraft:magic");
		helper.assertTrue(stranger.getAttachedOrCreate(ReinhardAttachments.STATE).wishesUsed() == 0
						&& stranger.getAttachedOrCreate(ReinhardAttachments.STATE).adaptedDamageTypes().isEmpty(),
				"wish-confirm from a non-Reinhard player is a no-op");
		TestPlayers.leave(stranger);

		ServerPlayer reinhard = TestPlayers.join(helper);
		TestHeroes.transform(reinhard, ReinhardHero.ID);
		reinhard.setAttached(ReinhardAttachments.STATE,
				reinhard.getAttachedOrCreate(ReinhardAttachments.STATE)
						.withRecentDamageTypes(List.of("minecraft:magic")));
		ReinhardWishAbility.handleWishConfirm(reinhard, "minecraft:magic");
		helper.assertTrue(reinhard.getAttachedOrCreate(ReinhardAttachments.STATE).wishesUsed() == 1
						&& reinhard.getAttachedOrCreate(ReinhardAttachments.STATE)
								.adaptedDamageTypes().contains("minecraft:magic"),
				"wish-confirm from Reinhard applies the wish");
		TestPlayers.leave(reinhard);
		helper.succeed();
	}
}
