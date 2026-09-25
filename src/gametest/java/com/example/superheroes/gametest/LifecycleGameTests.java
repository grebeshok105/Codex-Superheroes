package com.example.superheroes.gametest;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.AttributeModifierSet;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.hero.ScaramoucheHero;
import com.example.superheroes.lifecycle.ControlLockKind;
import com.example.superheroes.lifecycle.EntityControlLock;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.ThanosSnapWindupController;
import com.example.superheroes.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Audit B3 (ability-scoped modifiers are transient), B4 (control locks release on owner
 * leave / reconcile after unload), B8 (transform cooldown is attachment-backed), B17
 * (dead players drive no ability effects), B23 (symmetric hero runtime cleanup).
 */
public final class LifecycleGameTests implements FabricGameTest {
	private static final ResourceLocation TEST_MODIFIER =
			ResourceLocation.fromNamespaceAndPath(ModId.MOD_ID, "lifecycle_test");

	/** {@link AttributeInstance#save()} serializes permanent modifiers only. */
	private static boolean persists(AttributeInstance instance) {
		return instance.save().toString().contains(TEST_MODIFIER.toString());
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ownerLeaveRestoresControlLock(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, owner);
		helper.assertTrue(zombie.isNoAi(), "the lock applied NoAI");

		TestPlayers.leave(owner);

		helper.assertFalse(zombie.isNoAi(), "owner leaving restores the mob's own flag");
		helper.assertFalse(EntityControlLock.isLocked(zombie, ControlLockKind.NO_AI), "lock released");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void controlLockRefcountsAcrossOwners(GameTestHelper helper) {
		ServerPlayer first = TestPlayers.join(helper);
		ServerPlayer second = TestPlayers.join(helper);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, first);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, second);

		EntityControlLock.release(zombie, ControlLockKind.NO_AI, first.getUUID());
		helper.assertTrue(zombie.isNoAi(), "a second owner still holds the lock");

		EntityControlLock.release(zombie, ControlLockKind.NO_AI, second.getUUID());
		helper.assertFalse(zombie.isNoAi(), "last release restores the flag");
		TestPlayers.leave(first);
		TestPlayers.leave(second);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void reconcileRestoresFlagAfterLiveLockLost(GameTestHelper helper) {
		ServerPlayer owner = TestPlayers.join(helper);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, owner);
		// Unload simulation: the persisted flag and shadow survive, the live lock state does not.
		zombie.setAttached(ModAttachments.CONTROL_LOCKS, null);

		EntityControlLock.reconcile(zombie);

		helper.assertFalse(zombie.isNoAi(), "shadow restored the mob's own flag value");
		helper.assertTrue(zombie.getAttached(ModAttachments.CONTROL_LOCK_SHADOW) == null,
				"shadow consumed");
		TestPlayers.leave(owner);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void abilityScopedModifiersAreTransientNotPermanent(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		AttributeModifierSet transientSet = AttributeModifierSet.builder()
				.add(Attributes.MOVEMENT_SPEED, TEST_MODIFIER, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
				.abilityScoped()
				.build();
		transientSet.apply(player);

		AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
		helper.assertTrue(instance.getModifier(TEST_MODIFIER) != null, "modifier applied");
		helper.assertFalse(persists(instance), "ability-scoped modifiers never persist to NBT");
		transientSet.remove(player);
		helper.assertTrue(instance.getModifier(TEST_MODIFIER) == null, "modifier removed");

		AttributeModifierSet permanentSet = AttributeModifierSet.builder()
				.add(Attributes.MOVEMENT_SPEED, TEST_MODIFIER, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
				.build();
		permanentSet.apply(player);
		helper.assertTrue(persists(instance), "passive sets still apply as permanent");
		permanentSet.remove(player);
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void transformCooldownIsAttachmentBacked(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		helper.assertTrue(HeroTransformService.transform(player, RaidenHero.ID), "transformed");

		helper.assertTrue(player.getAttached(ModAttachments.TRANSFORM_TICK) != null,
				"the cooldown marker lives on the entity, not in a static map");
		helper.assertFalse(HeroTransformService.transform(player, ScaramoucheHero.ID),
				"transforming again inside the cooldown is rejected");
		helper.assertFalse(HeroTransformService.untransform(player),
				"untransform inside the cooldown is rejected");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void forceUntransformClearsRuntimeState(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		HeroTransformService.transform(player, RaidenHero.ID);
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.RAIDEN_SWORD_DRAW, 100);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, player);
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.RAIDEN_SWORD_DRAW),
				"cooldown armed");

		HeroTransformService.forceUntransform(player);

		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.RAIDEN_SWORD_DRAW),
				"cooldown deadlines persist — an untransform must not reset them");
		helper.assertFalse(zombie.isNoAi(), "held control locks released with the hero");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void snapNeverExecutesFromACorpse(GameTestHelper helper) {
		ServerPlayer caster = TestPlayers.join(helper);
		ServerPlayer victim = TestPlayers.join(helper);
		victim.teleportTo(caster.getX(), caster.getY(), caster.getZ());
		ThanosSnapWindupController.schedule(caster, 2, 200);
		caster.setHealth(0.0F);

		helper.runAfterDelay(10, () -> {
			helper.assertFalse(victim.hasEffect(ModEffects.SNAPPED),
					"a dead caster's snap must never fire");
			helper.succeed();
		});
	}
}
