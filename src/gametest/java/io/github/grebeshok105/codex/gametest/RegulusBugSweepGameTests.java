package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.hero.regulus.RegulusAttachments;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusBonusLife;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusTotemController;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Regulus rework Task 1 bug-sweep regressions — the freeze collision refusal, the
 * bonus-life cleanup in {@code clearMadness}, the totem's harmful-only effect strip,
 * and the shared owned/allied gate in {@link TargetFilters}.
 */
public final class RegulusBugSweepGameTests implements FabricGameTest {

	/**
	 * Two casters cannot stack on one victim: once a freeze owns the victim, a second
	 * caster's release is refused — the channel tears down and no steroids, locks or a
	 * second freeze entry are granted.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void secondCasterCannotStealFrozenVictim(GameTestHelper helper) {
		ServerPlayer caster1 = TestPlayers.join(helper, "greed-caster-1");
		ServerPlayer caster2 = TestPlayers.join(helper, "greed-caster-2");
		// The freeze costs 150 energy — the first caster must be a real Regulus
		// owner (the second is refused at the victim check before any charge).
		TestHeroes.transform(caster1, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(caster1);
		TestPlayers.clearSpawnInvulnerability(caster2);
		Zombie victim = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);

		TestPlayers.awaitVisible(helper, victim, () -> {
			RegulusGreedController.startMagnet(caster1, victim);
			RegulusGreedController.releaseAndFreeze(caster1);
			helper.assertTrue(RegulusGreedController.isFrozen(victim),
					"the first caster's freeze lands");
			helper.assertTrue(TestPlayers.lockOwners(victim, ControlLockKind.NO_AI)
					.contains(caster1.getUUID()), "the victim's AI lock belongs to caster 1");

			RegulusGreedController.startMagnet(caster2, victim);
			RegulusGreedController.releaseAndFreeze(caster2);

			helper.assertTrue(RegulusGreedController.isFrozen(victim),
					"the original freeze survives the refused second release");
			helper.assertFalse(TestPlayers.lockOwners(victim, ControlLockKind.NO_AI)
					.contains(caster2.getUUID()), "the refused caster holds no lock on the victim");
			helper.assertTrue(caster2.getEffect(MobEffects.DAMAGE_BOOST) == null,
					"the refused caster gets no freeze steroids");
			TestPlayers.leave(caster1);
			TestPlayers.leave(caster2);
			helper.succeed();
		});
	}

	/** The madness clear hook must consume a pending bonus life, not leak it into the next hero. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void clearMadnessConsumesBonusLife(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.setAttached(RegulusBonusLife.ATTACHMENT, Boolean.TRUE);

		RegulusMadnessController.clearMadness(player);

		helper.assertValueEqual(player.getAttachedOrCreate(RegulusAttachments.REGULUS_BONUS_LIFE),
				Boolean.FALSE, "clearMadness consumes the stored bonus life");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * The totem ward purges debuffs only: a beneficial hero effect on the player survives
	 * the revive while the harmful wither is stripped.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void totemReviveKeepsBeneficialEffects(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, RegulusHero.ID);
		TestPlayers.clearSpawnInvulnerability(player);
		// Regulus passives include infinite speed/boost/fire-resistance; pick a beneficial
		// effect the hero does NOT grant so the assertion cannot ride on a passive.
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9999, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.WITHER, 600, 0));

		player.kill();

		helper.assertTrue(player.isAlive(), "the ward refuses the first death");
		helper.assertTrue(RegulusTotemController.wasUsed(player.getUUID()),
				"the ward is marked used");
		helper.assertTrue(player.getEffect(MobEffects.DAMAGE_RESISTANCE) != null,
				"a beneficial effect survives the revive");
		helper.assertTrue(player.getEffect(MobEffects.WITHER) == null,
				"the harmful wither is stripped by the revive");
		TestPlayers.leave(player);
		helper.succeed();
	}

	/**
	 * A wolf tamed by the caster is owned and allied, so the shared hostile filter
	 * refuses it as a target; a wild wolf remains fair game.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void tamedWolfNotTargeted(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		Wolf tamed = helper.spawn(EntityType.WOLF, 2, 1, 2);
		tamed.tame(player);
		Wolf wild = helper.spawn(EntityType.WOLF, 4, 1, 4);

		helper.assertFalse(TargetFilters.harmableBy(tamed, player),
				"the owner's wolf is not a hostile target");
		helper.assertFalse(TargetFilters.hostileTo(player).test(tamed),
				"the scan predicate refuses the tamed wolf");
		helper.assertTrue(TargetFilters.harmableBy(wild, player),
				"a wild wolf is still a valid hostile target");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
