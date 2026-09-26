package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.lifecycle.PlayerLifecycle;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.hero.RaidenHero;
import io.github.grebeshok105.codex.hero.ScaramoucheHero;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.core.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.ThanosSnapWindupController;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
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
		helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
				.contains(owner.getUUID()), "the lock records the owner");

		// releaseOwnedBy resolves the victim by uuid — the spawn must be entity-visible first.
		TestPlayers.awaitVisible(helper, zombie, () -> {
			TestPlayers.leave(owner);

			helper.assertFalse(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
					.contains(owner.getUUID()), "owner leaving drops the owner's ref");
			helper.assertValueEqual(zombie.isNoAi(),
					!TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty(),
					"flag tracks the owners set");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void controlLockRefcountsAcrossOwners(GameTestHelper helper) {
		ServerPlayer first = TestPlayers.join(helper);
		ServerPlayer second = TestPlayers.join(helper);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, first);
		EntityControlLock.acquire(zombie, ControlLockKind.NO_AI, second);

		EntityControlLock.release(zombie, ControlLockKind.NO_AI, first.getUUID());
		helper.assertFalse(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
				.contains(first.getUUID()), "the released owner's ref is gone");
		helper.assertTrue(zombie.isNoAi(), "a second owner still holds the lock");

		EntityControlLock.release(zombie, ControlLockKind.NO_AI, second.getUUID());
		helper.assertFalse(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
				.contains(second.getUUID()), "last release drops the second ref");
		helper.assertValueEqual(zombie.isNoAi(),
				!TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty(),
				"flag tracks the owners set");
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
		zombie.setAttached(CoreAttachments.CONTROL_LOCKS, null);

		EntityControlLock.reconcile(zombie);

		helper.assertValueEqual(zombie.isNoAi(),
				!TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty(),
				"shadow restores the flag when no live lock remains");
		helper.assertTrue(zombie.getAttached(CoreAttachments.CONTROL_LOCK_SHADOW) == null,
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
		TestHeroes.transform(player, RaidenHero.ID);

		helper.assertTrue(player.getAttached(CoreAttachments.TRANSFORM_TICK) != null,
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

	/**
	 * D2c: the global AFTER_DEATH untransform asks {@code Hero.keepsHeroOnDeath()}.
	 * Doomsday adapts through death — his own AFTER_DEATH tiers him up and the
	 * transformation survives; a hero without the hook force-untransforms. The
	 * respawn half of the chain is driven through the same hook the dispatcher
	 * fires: {@code PlayerLifecycle.RESPAWN} → {@code HeroTransformService.onPlayerRespawn}
	 * (mock players have no real respawn round-trip; the call receives the same
	 * attachment-bearing entity the new player would).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void doomsdayKeepsHeroOnDeath(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper, "doomsday");
		ServerPlayer raiden = TestPlayers.join(helper, "raiden");
		TestHeroes.transform(doomsday, DoomsdayHero.ID);
		TestHeroes.transform(raiden, RaidenHero.ID);

		doomsday.kill();
		raiden.kill();

		helper.assertTrue(doomsday.getAttachedOrCreate(CoreAttachments.HERO_DATA).hasHero()
						&& DoomsdayHero.ID.equals(
								doomsday.getAttachedOrCreate(CoreAttachments.HERO_DATA).heroId()),
				"Doomsday keeps his transformation through death");
		helper.assertTrue(
				doomsday.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS).tier() == 2,
				"the death advanced his adaptation tier");
		helper.assertFalse(raiden.getAttachedOrCreate(CoreAttachments.HERO_DATA).hasHero(),
				"a hero without keepsHeroOnDeath untransforms on death");

		HeroTransformService.onPlayerRespawn(doomsday);

		helper.assertTrue(
				doomsday.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS).tier() == 2,
				"respawn re-apply keeps the tier his death granted");
		helper.assertTrue(doomsday.hasEffect(MobEffects.REGENERATION),
				"respawn re-applies Doomsday's tier effects");

		TestPlayers.leave(doomsday);
		TestPlayers.leave(raiden);
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
