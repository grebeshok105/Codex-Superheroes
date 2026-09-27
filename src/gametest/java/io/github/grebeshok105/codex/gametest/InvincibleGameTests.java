package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.effect.FlightController;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Characterization for the I3 module move, written against the pre-move layout:
 * Invincible's ability list, the shared Flight toggle it consumes, the shared
 * Viltrumite Recovery proc, and the Viltrumite Charge session lifetime
 * (self-expires after its tick duration, dies with the owner's session).
 */
public final class InvincibleGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void invincibleOwnsItsAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero invincible = Heroes.get(ModId.of("invincible"));
		helper.assertTrue(invincible != null, "invincible registered");
		helper.assertTrue(invincible.getAbilities().equals(List.of(
				ModId.of("flight"), ModId.of("viltrumite_charge"),
				ModId.of("viltrumite_recovery"), ModId.of("guardians_breaker"))),
				"slot order " + invincible.getAbilities());
		for (ResourceLocation id : invincible.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void flightTogglesOnAndOff(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("invincible"));
		ResourceLocation flight = ModId.of("flight");

		AbilityRouter.activate(player, flight);
		helper.assertTrue(HeroDataStore.get(player).isActive(flight), "flight toggled on");
		helper.assertTrue(FlightController.isFlightActive(HeroDataStore.get(player)),
				"flight controller engaged");

		// Flight is a toggle: a second activation deactivates it and stops the controller.
		AbilityRouter.activate(player, flight);
		helper.assertFalse(HeroDataStore.get(player).isActive(flight), "flight toggled off");
		helper.assertFalse(FlightController.isFlightActive(HeroDataStore.get(player)),
				"flight controller released");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void viltrumiteRecoveryHealsAndArmsCooldown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("invincible"));
		TestPlayers.clearSpawnInvulnerability(player);
		player.hurt(player.damageSources().generic(), 10f);
		float hurtHealth = player.getHealth();
		helper.assertTrue(hurtHealth < player.getMaxHealth(), "setup: the hit landed");

		AbilityRouter.activate(player, ModId.of("viltrumite_recovery"));
		helper.assertTrue(player.getHealth() > hurtHealth, "recovery heals 18 on activation");
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "regeneration applied");
		helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "absorption applied");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ModId.of("viltrumite_recovery")),
				"cooldown armed");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void viltrumiteChargeExpiresAfterItsDuration(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("invincible"));
		Ability charge = AbilityRegistry.get(ModId.of("viltrumite_charge"));

		AbilityRouter.activate(player, charge.getId());
		helper.assertFalse(charge.canActivate(player), "charge session started");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, charge.getId()), "cooldown armed");
		helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.1, "dash impulse applied");

		// The 4-tick dash is ticked by the module's player-phase row; after that the
		// session frees itself and canActivate returns true again.
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(charge.canActivate(player), "4-tick charge session is over");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void viltrumiteChargeSessionDiesWithLogout(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("invincible"));
		Ability charge = AbilityRegistry.get(ModId.of("viltrumite_charge"));

		AbilityRouter.activate(player, charge.getId());
		helper.assertFalse(charge.canActivate(player), "charge session started");
		TestPlayers.leave(player);
		helper.assertTrue(charge.canActivate(player), "logout cleared the charge session");
		helper.succeed();
	}
}
