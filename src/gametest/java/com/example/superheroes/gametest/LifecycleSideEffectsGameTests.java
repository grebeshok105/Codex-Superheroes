package com.example.superheroes.gametest;

import com.example.superheroes.ability.MirrorDimensionAbility;
import com.example.superheroes.ability.OmnimanThinkMarkAbility;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.BattleBeastCurseController;
import com.example.superheroes.effect.DoomGripController;
import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.effect.RegulusGreedController;
import com.example.superheroes.effect.RegulusMadnessState;
import com.example.superheroes.effect.RemDemonismController;
import com.example.superheroes.effect.SpatialBindController;
import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.lifecycle.ControlLockKind;
import com.example.superheroes.lifecycle.EntityControlLock;
import com.example.superheroes.lifecycle.HeroLifecycle;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;

/**
 * §6.3 characterization tests (stage D2b-2): clears and hooks that do more than drop a
 * static map — EntityControlLock releases, attachment resets, packet syncs, item returns —
 * keep running as explicit {@code ctx.lifecycle()} hooks after the migration; these tests
 * pin those side effects so a future OwnedSessionMap conversion cannot silently lose them.
 */
public final class LifecycleSideEffectsGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void doomGripClearReleasesVictimLock(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper, "doomgrip-owner");
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);

		DoomGripController.start(doomsday, zombie);
		helper.assertTrue(zombie.isNoAi(), "grip hard-locks the victim");
		helper.assertTrue(EntityControlLock.isLocked(doomsday, ControlLockKind.INVULNERABLE),
				"the caster is invulnerable while gripping");

		HeroLifecycle.fireClear(doomsday);

		helper.assertFalse(zombie.isNoAi(), "hero clear restores the victim's AI");
		helper.assertFalse(EntityControlLock.isLocked(zombie, ControlLockKind.NO_AI), "lock released");
		helper.assertFalse(EntityControlLock.isLocked(doomsday, ControlLockKind.INVULNERABLE),
				"caster invulnerability released");
		TestPlayers.leave(doomsday);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thinkMarkClearRestoresTargetFlags(GameTestHelper helper) {
		ServerPlayer omniman = TestPlayers.join(helper, "thinkmark-owner");
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		// Face the victim: put the caster two blocks behind it along -X, looking +X.
		omniman.teleportTo(zombie.getX() - 2.0, zombie.getY(), zombie.getZ(), -90.0F, 0.0F);

		boolean activated = new OmnimanThinkMarkAbility().tryActivate(omniman);
		helper.assertTrue(activated, "the grab must actually start for this test to say anything");
		helper.assertTrue(zombie.isNoAi(), "grab locks the victim's AI");

		HeroLifecycle.fireClear(omniman);

		helper.assertFalse(zombie.isNoAi(), "hero clear restores the victim's AI");
		helper.assertFalse(EntityControlLock.isLocked(zombie, ControlLockKind.NO_GRAVITY),
				"gravity lock released");
		TestPlayers.leave(omniman);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroClearEndsMadnessWithoutTouchingForeignEffects(GameTestHelper helper) {
		ServerPlayer regulus = TestPlayers.join(helper, "madness-owner");
		regulus.setAttached(ModAttachments.REGULUS_MADNESS,
				RegulusMadnessState.EMPTY.withMadness(true).withBonusLife(true));
		// Madness-applied instance: fixed amplifier 2, short 60t ambient duration (B12 contract).
		regulus.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false, true));
		// A foreign effect on another holder madness touches — must survive the clear.
		regulus.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9999, 5, false, false, false));

		HeroLifecycle.fireClear(regulus);

		RegulusMadnessState state = regulus.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
		helper.assertFalse(state.madness(), "madness state reset to EMPTY");
		helper.assertTrue(regulus.getEffect(MobEffects.MOVEMENT_SPEED) == null,
				"the madness-owned speed instance was removed");
		MobEffectInstance resist = regulus.getEffect(MobEffects.DAMAGE_RESISTANCE);
		helper.assertTrue(resist != null && resist.getAmplifier() == 5,
				"the foreign amplifier-5 resistance survived — madness only strips its own instances");
		TestPlayers.leave(regulus);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ownerLeaveReleasesGreedFrozenVictim(GameTestHelper helper) {
		ServerPlayer regulus = TestPlayers.join(helper, "greed-owner");
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);

		RegulusGreedController.startMagnet(regulus, zombie);
		RegulusGreedController.releaseAndFreeze(regulus);
		helper.assertTrue(zombie.isNoAi(), "releaseAndFreeze locks the victim");

		TestPlayers.leave(regulus);

		helper.assertFalse(zombie.isNoAi(), "caster leaving frees the frozen victim");
		helper.assertFalse(EntityControlLock.isLocked(zombie, ControlLockKind.NO_AI), "lock released");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void casterLeaveClosesMirrorHouse(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "house-caster");
		ServerPlayer victim = TestPlayers.join(helper, "house-victim");
		victim.teleportTo(pandora.getX() + 2.0, pandora.getY(), pandora.getZ());

		MirrorDimensionController.start(pandora, MirrorDimensionAbility.ACID_MODE,
				MirrorDimensionAbility.ACID_SCALE);
		helper.assertTrue(MirrorDimensionController.hasActiveHouse(pandora), "the House opened");
		SpatialBindController.bind(pandora, victim);
		helper.assertTrue(SpatialBindController.isBound(victim), "victim rope-bound");

		TestPlayers.leave(pandora);

		helper.assertFalse(MirrorDimensionController.hasActiveHouse(pandora),
				"caster leaving closes the House");
		helper.assertFalse(SpatialBindController.isBound(victim), "the victim's ropes fall away");
		TestPlayers.leave(victim);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void victimLeaveDropsOutOfMirrorHouse(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "house-caster2");
		ServerPlayer victim = TestPlayers.join(helper, "house-victim2");
		victim.teleportTo(pandora.getX() + 2.0, pandora.getY(), pandora.getZ());

		MirrorDimensionController.start(pandora, MirrorDimensionAbility.ACID_MODE,
				MirrorDimensionAbility.ACID_SCALE);
		helper.assertTrue(MirrorDimensionController.isTrapped(victim), "victim absorbed by the House");

		TestPlayers.leave(victim);

		helper.assertFalse(MirrorDimensionController.isTrapped(victim),
				"a leaving victim is released from the House");
		TestPlayers.leave(pandora);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroClearStripsPandoraRevival(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper, "pandora-owner");
		pandora.setAttached(ModAttachments.PANDORA_REVIVED, Boolean.TRUE);
		EntityControlLock.acquire(pandora, ControlLockKind.INVULNERABLE, pandora);
		helper.assertTrue(EntityControlLock.isLocked(pandora, ControlLockKind.INVULNERABLE),
				"revival invulnerability held");

		HeroLifecycle.fireClear(pandora);

		helper.assertTrue(pandora.getAttached(ModAttachments.PANDORA_REVIVED) == null,
				"revived attachment cleared on hero clear");
		helper.assertFalse(EntityControlLock.isLocked(pandora, ControlLockKind.INVULNERABLE),
				"permanent invulnerability released");
		TestPlayers.leave(pandora);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void relogReappliesBattleBeastCurse(GameTestHelper helper) {
		ServerPlayer bb = TestPlayers.join(helper, "battlebeast");
		TestHeroes.transform(bb, BattleBeastHero.ID);
		BattleBeastCurseController.setStage(bb, 5);
		double cursedArmor = bb.getAttributeValue(Attributes.ARMOR);
		helper.assertTrue(cursedArmor > 0, "the curse granted armor at stage 5");

		TestPlayers.leave(bb);
		ServerPlayer relogged = TestPlayers.rejoin(helper, bb);

		helper.assertTrue(relogged.getAttributeValue(Attributes.ARMOR) >= cursedArmor,
				"the relogged BattleBeast got its curse modifiers back via the join hook");
		TestPlayers.leave(relogged);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroClearReturnsRemMace(GameTestHelper helper) {
		ServerPlayer rem = TestPlayers.join(helper, "rem-owner");
		RemDemonismController.giveMace(rem);
		helper.assertTrue(TestPlayers.count(rem, ModItems.REM_MORNING_STAR) > 0, "the mace was issued");

		HeroLifecycle.fireClear(rem);

		helper.assertTrue(TestPlayers.count(rem, ModItems.REM_MORNING_STAR) == 0,
				"hero clear returns the bound mace — an item side effect, not a map drop");
		TestPlayers.leave(rem);
		helper.succeed();
	}
}
