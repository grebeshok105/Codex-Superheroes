package com.example.superheroes.gametest;

import com.example.superheroes.effect.RegulusMadnessController;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.hero.RegulusHero;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Audit B12 (hero passives are re-asserted after effect wipes; {@code clearMadness} drops only
 * madness-owned instances) and B16 (a Regulus counter suppresses fall immunity only for its
 * participants, not globally).
 */
public final class PassivesFallGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void milkWipeRestoresHeroPassives(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "passives applied at transform");

		// the vanilla milk path: removeAllEffects fires onEffectRemoved per instance
		player.removeAllEffects();
		helper.assertTrue(!player.hasEffect(MobEffects.REGENERATION), "the wipe removed the passives");

		helper.runAfterDelay(2, () -> {
			for (var effect : java.util.List.of(MobEffects.REGENERATION, MobEffects.MOVEMENT_SPEED,
					MobEffects.DAMAGE_BOOST, MobEffects.JUMP, MobEffects.FIRE_RESISTANCE)) {
				MobEffectInstance instance = player.getEffect(effect);
				helper.assertTrue(instance != null && instance.isInfiniteDuration(),
						"declared passive restored after the wipe: " + effect.unwrapKey().orElseThrow());
			}
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroSwapReplacesDeclaredPassives(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);

		helper.runAfterDelay(25, () -> {
			TestHeroes.transform(player, KratosHero.ID);
			player.removeAllEffects();
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(!player.hasEffect(MobEffects.REGENERATION),
					"the previous hero's passives stay gone after a swap");
			MobEffectInstance resistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
			helper.assertTrue(resistance != null && resistance.isInfiniteDuration(),
					"the new hero's passives are re-asserted, not the old one's");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void clearMadnessKeepsPotionBeaconAndPassives(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);

		// vanilla potion: ambient=false, visible=true — must survive clearMadness (audit B12)
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0, false, true, true));
		// beacon-style: ambient=true but visible — also not madness-owned
		player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, true, true, true));
		// madness-owned (60t, ambient, icon-only, the madness amplifier) — must be dropped;
		// they merge over the infinite passives (amp1), so vanilla nests the passive underneath
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 2, true, false, true));

		RegulusMadnessController.clearMadness(player);

		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
				"a potion effect survives clearMadness");
		helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING),
				"a beacon-style effect survives clearMadness");
		helper.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SPEED),
				"the madness-owned SPEED instance is removed even though a passive hides under it");
		helper.assertTrue(!player.hasEffect(MobEffects.JUMP),
				"the madness-owned JUMP instance is removed even though a passive hides under it");
		MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
		helper.assertTrue(strength != null && strength.isInfiniteDuration() && strength.getAmplifier() == 1,
				"the infinite hero passive is not madness-owned and survives");

		helper.runAfterDelay(2, () -> {
			MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
			helper.assertTrue(jump != null && jump.isInfiniteDuration() && jump.getAmplifier() == 1,
					"the reconciler restores the passive that was nested under the removed madness effect");
			MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
			helper.assertTrue(speed != null && speed.isInfiniteDuration() && speed.getAmplifier() == 1,
					"the reconciler restores the SPEED passive too");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void counterStripsFallImmunityOnlyForParticipants(GameTestHelper helper) {
		ServerPlayer regulus = TestPlayers.join(helper);
		ServerPlayer bystander = TestPlayers.join(helper);
		TestHeroes.transform(regulus, RegulusHero.ID);
		TestHeroes.transform(bystander, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(regulus);
		TestPlayers.clearSpawnInvulnerability(bystander);
		Zombie victim = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);

		helper.assertTrue(!regulus.causeFallDamage(10f, 1f, helper.getLevel().damageSources().fall()),
				"baseline: fall immunity cancels fall damage before the counter");

		RegulusMadnessController.triggerCounter(regulus, victim);
		helper.assertTrue(RegulusMadnessController.isCounterInvolved(regulus)
						&& RegulusMadnessController.isCounterInvolved(victim)
						&& !RegulusMadnessController.isCounterInvolved(bystander),
				"the counter involves only its owner and its victim");

		helper.assertTrue(regulus.causeFallDamage(10f, 1f, helper.getLevel().damageSources().fall()),
				"a participant of the counter takes fall damage while it runs");
		helper.assertTrue(!bystander.causeFallDamage(10f, 1f, helper.getLevel().damageSources().fall()),
				"an uninvolved hero keeps fall immunity (audit B16)");

		RegulusMadnessController.clearMadness(regulus);
		TestPlayers.leave(regulus);
		TestPlayers.leave(bystander);
		helper.succeed();
	}
}
