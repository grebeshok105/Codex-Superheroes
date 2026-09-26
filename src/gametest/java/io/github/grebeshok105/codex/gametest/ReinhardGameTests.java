package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.effect.ReinhardState;
import io.github.grebeshok105.codex.hero.ReinhardHero;
import io.github.grebeshok105.codex.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Characterization pins for the G1 Reinhard module move: written against the pre-move
 * layout and must pass identically after {@code hero/reinhard/} absorbs the server side.
 *
 * <p>Sword gate: {@code AbilityIds.isReinhardSwordOnly} is dead code — a repo-wide
 * call-site search finds no caller (not the router, not the abilities, not HUD
 * visibility), so today the four "sword-only" abilities activate with empty hands:
 * the energy is charged and the effects run. The only sword-related refusal in the
 * activation path is {@code reinhard_sword_draw} itself: without the damage gate's
 * ready flag its {@code tryActivate} shows {@code ability.superheroes.reinhard_sword_draw.no_worthy}
 * and returns {@code false} — for the router a silent refusal (no toggle, no charge).
 */
public final class ReinhardGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void swordOnlyAbilitiesRequireTheSword(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		helper.assertTrue(TestPlayers.count(player, ModItems.ROYAL_ICICLE) == 0,
				"fresh Reinhard holds no Royal Icicle");
		float energy0 = HeroDataStore.get(player).energy();

		AbilityRouter.activate(player, AbilityIds.REINHARD_AIR_SLASH);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.REINHARD_AIR_SLASH),
				"no gate stops Air Slash without the sword");
		helper.assertTrue(HeroDataStore.get(player).energy() == energy0 - 200f,
				"Air Slash charged 200 energy");

		AbilityRouter.activate(player, AbilityIds.REINHARD_SWORD_WAVE);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.REINHARD_SWORD_WAVE),
				"no gate stops Sword Wave without the sword");
		helper.assertTrue(HeroDataStore.get(player).energy() == energy0 - 450f,
				"Sword Wave charged another 250");

		AbilityRouter.activate(player, AbilityIds.REINHARD_COUNTER_RIPOSTE);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.REINHARD_COUNTER_RIPOSTE),
				"no gate stops Counter Riposte without the sword");
		helper.assertTrue(player.getAttachedOrCreate(ModAttachments.REINHARD_STATE).riposteExpireTick()
						> player.serverLevel().getGameTime(),
				"the riposte window armed");

		AbilityRouter.activate(player, AbilityIds.REINHARD_DIVINE_AURA);
		helper.assertTrue(HeroDataStore.get(player).isActive(AbilityIds.REINHARD_DIVINE_AURA),
				"no gate stops Divine Aura without the sword");

		// The only sword-side denial that exists today: the draw itself is refused until
		// ReinhardSwordDrawGateController marks him ready (30 damage from one attacker
		// inside 30 s). tryActivate fails with the no_worthy message and no sword is issued.
		AbilityRouter.activate(player, AbilityIds.REINHARD_SWORD_DRAW);
		helper.assertFalse(HeroDataStore.get(player).isActive(AbilityIds.REINHARD_SWORD_DRAW),
				"the draw is refused while the damage gate is not ready");
		helper.assertFalse(player.getAttachedOrCreate(ModAttachments.REINHARD_STATE).swordDrawn(),
				"swordDrawn stays false");
		helper.assertTrue(TestPlayers.count(player, ModItems.ROYAL_ICICLE) == 0,
				"no Royal Icicle was issued");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * {@code REINHARD_STATE} is persistent + copyOnDeath: fields the hero-clear does not
	 * reset (spent wishes, the judgment mark) stay on the player through death, while
	 * {@code clearAdaptations} — fired by the death-triggered untransform — drops the
	 * adaptations and the drawn flag. Mock players have no real respawn round-trip, so
	 * the respawn half is driven through the same hook the dispatcher fires
	 * (convention from {@code LifecycleGameTests.doomsdayKeepsHeroOnDeath}).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stateSurvivesDeath(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ReinhardHero.ID);
		UUID mark = UUID.randomUUID();
		player.setAttached(ModAttachments.REINHARD_STATE,
				player.getAttachedOrCreate(ModAttachments.REINHARD_STATE)
						.withWishesUsed(2)
						.withJudgmentTarget(Optional.of(mark), 4242L)
						.withAdaptedDamageTypes(List.of("minecraft:magic"))
						.withSwordDrawn(true)
						// Phoenix spent, otherwise Second Coming intercepts the kill.
						.withPhoenixUsed(true));

		player.kill();
		helper.assertFalse(player.isAlive(), "phoenix spent — the kill is real");
		HeroTransformService.onPlayerRespawn(player);

		ReinhardState after = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
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
}
