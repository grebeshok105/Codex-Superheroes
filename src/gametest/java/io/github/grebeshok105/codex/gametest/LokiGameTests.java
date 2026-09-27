package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

import java.util.List;

/**
 * Pins for the I1b Loki module move: written against the pre-move layout and must
 * pass identically after {@code hero/loki/} absorbs the server side. Ability ids are
 * literals because the constants move from {@code AbilityIds} to the module.
 */
public final class LokiGameTests implements FabricGameTest {
	private static final ResourceLocation ASTRAL_CLONES = ModId.of("loki_astral_clones");
	private static final ResourceLocation TESSERACT_BLINK = ModId.of("loki_tesseract_blink");
	private static final ResourceLocation MIND_CHARM = ModId.of("loki_mind_charm");
	private static final ResourceLocation GLAMOUR = ModId.of("loki_glamour");
	private static final ResourceLocation CHAOS_BOLT = ModId.of("loki_chaos_bolt");

	/**
	 * Spawn a zombie {@code dist} blocks ahead of the player. {@code helper.spawn}
	 * places entities at structure coordinates while {@code TestPlayers.join} puts
	 * the player at world spawn millions of blocks away — proximity scans and
	 * SafeTeleport's loaded-chunk check only work near the player.
	 */
	private static Zombie spawnZombieAhead(ServerPlayer player, double dist) {
		var level = player.serverLevel();
		Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
		var pos = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(dist));
		// Force-load the destination chunk first: in an unloaded chunk the entity is
		// never registered for area scans.
		level.getChunk(BlockPos.containing(pos.x, pos.y, pos.z));
		zombie.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
		level.addFreshEntity(zombie);
		return zombie;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void lokiOwnsExactlyItsFiveAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero loki = Heroes.get(ModId.of("loki"));
		helper.assertTrue(loki != null, "loki registered");
		helper.assertTrue(loki.getAbilities().equals(List.of(ASTRAL_CLONES, TESSERACT_BLINK,
				MIND_CHARM, GLAMOUR, CHAOS_BOLT)), "slot order " + loki.getAbilities());
		for (ResourceLocation id : loki.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void glamourToggleAppliesAndStripsBuffs(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("loki"));

		AbilityRouter.activate(player, GLAMOUR);
		helper.assertTrue(HeroDataStore.get(player).isActive(GLAMOUR), "glamour went active");
		helper.assertTrue(player.hasEffect(MobEffects.INVISIBILITY), "glamour invisibility applied");
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "glamour speed applied");
		helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST), "glamour strength applied");
		helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "glamour haste applied");

		AbilityRouter.deactivate(player, GLAMOUR);
		helper.assertFalse(HeroDataStore.get(player).isActive(GLAMOUR), "glamour went inactive");
		helper.assertFalse(player.hasEffect(MobEffects.INVISIBILITY), "invisibility removed");
		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "speed removed");
		helper.assertFalse(player.hasEffect(MobEffects.DAMAGE_BOOST), "strength removed");
		helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "haste removed");
		helper.assertFalse(AbilityCooldowns.isOnCooldown(player, GLAMOUR),
				"glamour deactivation sets no cooldown");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void astralClonesDisorientsMobsAndHidesCaster(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("loki"));
		Zombie zombie = spawnZombieAhead(player, 4.0);

		// The clone wave scans entity sections — wait for the spawn to be visible.
		TestPlayers.awaitVisible(helper, zombie, () -> {
			zombie.setTarget(player);
			AbilityRouter.activate(player, ASTRAL_CLONES);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, ASTRAL_CLONES),
					"astral clones went on cooldown");
			helper.assertTrue(zombie.getTarget() == null, "clones drop the zombie's target");
			helper.assertTrue(zombie.hasEffect(MobEffects.CONFUSION), "clone confusion applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.BLINDNESS), "clone blindness applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "clone weakness applied");
			helper.assertTrue(player.hasEffect(MobEffects.INVISIBILITY), "caster went invisible");
			helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "caster speed applied");
			helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "caster resistance applied");
			helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "caster haste applied");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void mindCharmCharmsTargetAhead(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("loki"));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		// Face the victim: put the caster two blocks behind it along -X, looking +X.
		player.teleportTo(zombie.getX() - 2.0, zombie.getY(), zombie.getZ());
		player.setYRot(-90.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(-90.0F);
		player.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			zombie.setTarget(player);
			AbilityRouter.activate(player, MIND_CHARM);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, MIND_CHARM),
					"mind charm went on cooldown");
			helper.assertTrue(zombie.getTarget() == null, "charm drops the zombie's target");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "charm weakness applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "charm slowness applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING), "charm glowing applied");
			helper.assertTrue(zombie.hasEffect(MobEffects.LEVITATION), "charm levitation applied");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chaosBoltKillsWeakTargetAhead(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("loki"));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		player.teleportTo(zombie.getX() - 3.0, zombie.getY(), zombie.getZ());
		player.setYRot(-90.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(-90.0F);
		player.setYBodyRot(-90.0F);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, CHAOS_BOLT);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, CHAOS_BOLT),
					"chaos bolt went on cooldown");
			helper.assertFalse(zombie.isAlive(), "28 chaos damage kills a 20hp zombie");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "bolt weakness applied");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tesseractBlinkTeleportsBehindTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("loki"));
		player.setYRot(-90.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(-90.0F);
		player.setYBodyRot(-90.0F);
		// Isolate actor + victim from the shared level: foreign AoE damage grants the
		// zombie its 10-tick invulnerability window and the backstab then no-ops.
		double isoX = player.getX() + 2000.0;
		double isoZ = player.getZ() + 2000.0;
		helper.getLevel().getChunk(BlockPos.containing(isoX, player.getY(), isoZ));
		// Both actors need a floor at the iso spot — in the void they fall during
		// awaitVisible and the 3D distance crosses the assert threshold.
		helper.getLevel().setBlock(BlockPos.containing(isoX, player.getY() - 1, isoZ), Blocks.STONE.defaultBlockState(), 3);
		player.teleportTo(isoX, player.getY(), isoZ);
		Zombie zombie = spawnZombieAhead(player, 4.0);
		helper.getLevel().setBlock(zombie.blockPosition().below(), Blocks.STONE.defaultBlockState(), 3);
		// Pin the landing geometry: a chasing/wandering zombie drifts past the
		// 2.5-block assert before the blink resolves.
		zombie.setNoAi(true);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, TESSERACT_BLINK);

			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, TESSERACT_BLINK),
					"blink went on cooldown");
			helper.assertTrue(player.distanceTo(zombie) < 2.5,
					"targeted blink lands on the victim, got " + player.distanceTo(zombie));
			helper.assertFalse(zombie.isAlive(), "backstab kills a 20hp zombie");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}
}
