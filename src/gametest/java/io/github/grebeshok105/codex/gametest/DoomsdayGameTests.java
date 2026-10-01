package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.doomsday.DoomsdayAttachments;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.core.model.ControlLockKind;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.hero.homelander.registry.HomelanderDamageTypes;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomGripController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayAdaptationController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayEffectAdaptationController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayProgress;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayTierController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayModifiers;
import io.github.grebeshok105.codex.hero.doomsday.item.KryptoniteShardItem;
import io.github.grebeshok105.codex.hero.doomsday.DoomsdayItems;
import io.github.grebeshok105.codex.hero.doomsday.net.DoomsdayProgressS2CPayload;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pins for the I4d Doomsday module move: written against the pre-move layout and must pass
 * identically after {@code hero/doomsday/} absorbs the server side. Hero/ability ids are
 * literals because the constants move from {@code AbilityIds}/shared registries to the module.
 *
 * <p>Behaviour under pin: tier-gated slot order, genome transform/untransform, cumulative
 * adaptation learning + related-group immunity (incl. the boss-key group boundaries the
 * {@code #superheroes:beam} refactor must preserve), adaptation persistence across relog and
 * lethal death, kryptonite shard drop/pickup/cleanse, the {@code /superheroes doomsday tier}
 * command surface that moves to the new {@code commands()} seam, and doom-grip lock lifecycle.
 */
public final class DoomsdayGameTests implements FabricGameTest {
	private static final ResourceLocation DOOMSDAY = ModId.of("doomsday");
	private static final ResourceLocation DOOMSDAY_SMASH = ModId.of("doomsday_smash");
	private static final ResourceLocation DOOMSDAY_ROAR = ModId.of("doomsday_roar");
	private static final ResourceLocation DOOMSDAY_BONE_SPIKE = ModId.of("doomsday_bone_spike");
	private static final ResourceLocation DOOMSDAY_CHARGE_TACKLE = ModId.of("doomsday_charge_tackle");
	private static final ResourceLocation DOOMSDAY_BERSERK = ModId.of("doomsday_berserk");
	private static final ResourceLocation DOOMSDAY_DOOM_GRIP = ModId.of("doomsday_doom_grip");

	private static ResourceKey<DamageType> bossKey(String path) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(path));
	}

	private static DamageSource bossDamage(ServerLevel level, String path) {
		Holder<DamageType> holder = level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(bossKey(path));
		return new DamageSource(holder);
	}

	private static DoomsdayProgress progress(ServerPlayer player) {
		return player.getAttachedOrCreate(DoomsdayAttachments.PROGRESS);
	}

	private static void hit(ServerPlayer player, DamageSource source, float amount) {
		player.invulnerableTime = 0;
		player.hurt(source, amount);
	}

	// ─────────────────────────── roster / transform ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void doomsdayOwnsItsSixAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero hero = Heroes.get(DOOMSDAY);
		helper.assertTrue(hero != null, "doomsday registered");
		helper.assertValueEqual(hero.getAbilities(), List.of(DOOMSDAY_SMASH, DOOMSDAY_ROAR,
				DOOMSDAY_BONE_SPIKE, DOOMSDAY_CHARGE_TACKLE, DOOMSDAY_BERSERK, DOOMSDAY_DOOM_GRIP),
				"slot order");
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.assertTrue(hero.keepsHeroOnDeath(), "doomsday keeps his hero through death");
		helper.assertTrue(hero.getImpactStyle() == ImpactStyle.BRUTAL, "BRUTAL impact style");
		helper.assertTrue(hero.getThreatClass() == JarvisThreatClass.S, "S threat class");
		helper.assertTrue(hero.getEnergyMax() == 200f && hero.getManaMax() == 0f,
				"energy 200 / no mana");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void genomeItemTransformsAndShiftUseUntransforms(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DoomsdayItems.DOOMSDAY_GENOME));

		player.setShiftKeyDown(true);
		InteractionResultHolder<ItemStack> noHeroUse =
				DoomsdayItems.DOOMSDAY_GENOME.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(noHeroUse.getResult() == InteractionResult.FAIL,
				"shift-use with no hero does nothing");

		player.setShiftKeyDown(false);
		InteractionResultHolder<ItemStack> transformed =
				DoomsdayItems.DOOMSDAY_GENOME.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(transformed.getResult() == InteractionResult.CONSUME,
				"use transforms into doomsday");
		helper.assertValueEqual(HeroDataStore.get(player).heroId(), DOOMSDAY, "hero is doomsday");
		helper.assertTrue(progress(player).tier() == 1, "fresh transformation starts at tier 1");

		player.setShiftKeyDown(true);
		InteractionResultHolder<ItemStack> insideCooldown =
				DoomsdayItems.DOOMSDAY_GENOME.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(insideCooldown.getResult() == InteractionResult.FAIL,
				"shift-use inside the 20-tick transform cooldown is rejected");

		helper.runAfterDelay(21, () -> {
			InteractionResultHolder<ItemStack> untransformed =
					DoomsdayItems.DOOMSDAY_GENOME.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			helper.assertTrue(untransformed.getResult() == InteractionResult.CONSUME,
					"shift-use after the cooldown untransforms");
			helper.assertFalse(HeroDataStore.get(player).hasHero(), "hero cleared");
			helper.assertValueEqual(progress(player), DoomsdayProgress.EMPTY,
					"untransform resets the doomsday progress attachment");
			player.setShiftKeyDown(false);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── adaptation learning ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void accumulatedDamageTeachesTypeImmunity(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);
		TestPlayers.clearSpawnInvulnerability(doomsday);
		DamageSource freeze = helper.getLevel().damageSources().freeze();

		for (int i = 0; i < 4; i++) {
			hit(doomsday, freeze, 4.0f); // 16 cumulative ≥ 15 threshold
		}
		helper.assertTrue(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
				"cumulative damage past the threshold teaches immunity");
		helper.assertTrue(DoomsdayAdaptationController.getAdaptationCount(doomsday) == 1,
				"one adaptation recorded");
		AttributeInstance attack = doomsday.getAttribute(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(attack != null
						&& attack.getModifier(DoomsdayModifiers.DOOMSDAY_ADAPT_DAMAGE) != null
						&& attack.getModifier(DoomsdayModifiers.DOOMSDAY_ADAPT_DAMAGE).amount() == 1.0,
				"each adaptation grants +1 attack damage");

		doomsday.invulnerableTime = 0;
		helper.assertFalse(doomsday.hurt(freeze, 4.0f), "adapted damage type is denied");
		helper.assertTrue(doomsday.isAlive(), "the adapted hit dealt no damage");

		doomsday.invulnerableTime = 0;
		helper.assertTrue(doomsday.hurt(helper.getLevel().damageSources().drown(), 1.0f),
				"an unrelated type still lands");
		TestPlayers.leave(doomsday);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void genericPlayerAttackNeverTeachesImmunity(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		ServerPlayer attacker = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);
		TestPlayers.clearSpawnInvulnerability(doomsday);
		DamageSource attack = helper.getLevel().damageSources().playerAttack(attacker);

		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.getLevel().getServer().setPvpAllowed(true);
		try {
			for (int i = 0; i < 4; i++) {
				hit(doomsday, attack, 4.0f);
			}
			helper.assertFalse(DoomsdayAdaptationController.hasAdapted(doomsday, attack),
					"player_attack is a generic type — no immunity is ever written");
			helper.assertTrue(DoomsdayAdaptationController.getAdaptationCount(doomsday) == 0,
					"generic damage leaves the adapt counter at zero");
			doomsday.invulnerableTime = 0;
			helper.assertTrue(doomsday.hurt(attack, 1.0f), "generic damage keeps landing");
		} finally {
			helper.getLevel().getServer().setPvpAllowed(oldPvp);
		}
		TestPlayers.leave(doomsday);
		TestPlayers.leave(attacker);
		helper.succeed();
	}

	/**
	 * The boss-key {@code RELATED_GROUPS} site the {@code #superheroes:beam} refactor moves:
	 * adapting to {@code homelander_eye_laser} denies the whole group (eye_laser, heat_vision,
	 * lightning_call) but not the clap/slam group, and plain {@code superheroes:eye_laser}
	 * (Iron Man's beam, a different key in {@code #beam}) is unaffected.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void bossBeamAdaptationBlocksOnlyItsSiblingGroup(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);
		TestPlayers.clearSpawnInvulnerability(doomsday);
		ServerLevel level = helper.getLevel();

		DoomsdayAdaptationController.registerAdaptation(doomsday, bossKey("homelander_eye_laser"), false);

		helper.assertFalse(doomsday.hurt(bossDamage(level, "homelander_eye_laser"), 1.0f),
				"the adapted key is denied");
		doomsday.invulnerableTime = 0;
		helper.assertFalse(doomsday.hurt(bossDamage(level, "homelander_heat_vision"), 1.0f),
				"homelander_heat_vision shares the group");
		doomsday.invulnerableTime = 0;
		helper.assertFalse(doomsday.hurt(bossDamage(level, "homelander_lightning_call"), 1.0f),
				"homelander_lightning_call shares the group even though it is not in #superheroes:beam");
		doomsday.invulnerableTime = 0;
		helper.assertTrue(doomsday.hurt(bossDamage(level, "homelander_hand_clap"), 1.0f),
				"the clap/slam boss group is a different group — it still lands");
		doomsday.invulnerableTime = 0;
		// Null attacker: a player attacker would trip the shared-level pvp=false gate before
		// adaptation is even consulted (eye_laser is a player-usable beam).
		helper.assertTrue(doomsday.hurt(HomelanderDamageTypes.eyeLaser(level, null), 1.0f),
				"superheroes:eye_laser is in #beam but not in this group — it still lands");
		TestPlayers.leave(doomsday);
		helper.succeed();
	}

	// ─────────────────────────── persistence ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void adaptationsSurviveRelog(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper, "doomsday-relog");
		TestHeroes.transform(doomsday, DOOMSDAY);
		DamageSource freeze = helper.getLevel().damageSources().freeze();
		DoomsdayAdaptationController.registerAdaptation(doomsday, DamageTypes.FREEZE, false);
		helper.assertTrue(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
				"adaptation recorded before relog");

		TestPlayers.leave(doomsday);
		ServerPlayer back = TestPlayers.rejoin(helper, doomsday);

		helper.assertValueEqual(HeroDataStore.get(back).heroId(), DOOMSDAY,
				"rejoin keeps the doomsday hero");
		helper.assertTrue(DoomsdayAdaptationController.hasAdapted(back, freeze),
				"the adaptation table is keyed by uuid — it survives the relog");
		TestPlayers.clearSpawnInvulnerability(back);
		back.invulnerableTime = 0;
		helper.assertFalse(back.hurt(freeze, 2.0f), "the relogged doomsday is still immune");
		TestPlayers.leave(back);
		helper.succeed();
	}

	/**
	 * Death by a tracked damage type tiers him up, records the source key, writes the lethal
	 * adaptation and schedules the respawn relocate; the AFTER_RESPAWN chain re-applies the
	 * new tier and clears the pending relocate. {@code ServerPlayerEvents.AFTER_RESPAWN} is
	 * invoked directly — the convention for mock players (no real respawn round-trip).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lethalDeathGrantsTierAndImmunity(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);
		TestPlayers.clearSpawnInvulnerability(doomsday);
		DamageSource freeze = helper.getLevel().damageSources().freeze();

		doomsday.hurt(freeze, 1000f);
		helper.assertFalse(doomsday.isAlive(), "the kill is real");

		DoomsdayProgress afterDeath = progress(doomsday);
		helper.assertTrue(afterDeath.tier() == 2, "death tiers him up: " + afterDeath.tier());
		helper.assertTrue(afterDeath.deathSources().contains("minecraft:freeze"),
				"the killing damage type is recorded: " + afterDeath.deathSources());
		helper.assertTrue(afterDeath.pendingRelocate(), "respawn relocation is scheduled");
		helper.assertTrue(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
				"death teaches immunity to the killing type");
		helper.assertValueEqual(HeroDataStore.get(doomsday).heroId(), DOOMSDAY,
				"keepsHeroOnDeath — still doomsday");

		ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(doomsday, doomsday, false);

		DoomsdayProgress afterRespawn = progress(doomsday);
		helper.assertTrue(afterRespawn.tier() == 2, "respawn keeps the earned tier");
		helper.assertFalse(afterRespawn.pendingRelocate(), "relocate flag consumed on respawn");
		helper.assertTrue(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
				"the lethal adaptation persists through respawn");
		TestPlayers.leave(doomsday);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untransformWipesProgressAndAdaptations(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, DOOMSDAY);
		DoomsdayTierController.setTier(player, 5);
		DoomsdayAdaptationController.registerAdaptation(player, DamageTypes.FREEZE, false);
		helper.assertTrue(progress(player).tier() == 5
						&& DoomsdayAdaptationController.hasAdapted(
								player, helper.getLevel().damageSources().freeze()),
				"tier and adaptation staged");

		HeroTransformService.forceUntransform(player);

		helper.assertFalse(HeroDataStore.get(player).hasHero(), "hero cleared");
		helper.assertValueEqual(progress(player), DoomsdayProgress.EMPTY,
				"progress reset to EMPTY");
		helper.assertFalse(DoomsdayAdaptationController.hasAdapted(
						player, helper.getLevel().damageSources().freeze()),
				"adaptation table cleared");
		helper.assertTrue(DoomsdayAdaptationController.getAdaptationCount(player) == 0,
				"adapt counter cleared");
		AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(attack == null
						|| attack.getModifier(DoomsdayModifiers.DOOMSDAY_ADAPT_DAMAGE) == null,
				"the adapt damage bonus is removed");
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ─────────────────────────── kryptonite ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void thirtyAttackerDamageDropsBoundShard(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper, "doomsday-shard");
		ServerPlayer attacker = TestPlayers.join(helper, "shard-owner");
		TestHeroes.transform(doomsday, DOOMSDAY);
		DoomsdayTierController.setTier(doomsday, 5); // shards only drop from tier 4+
		TestPlayers.clearSpawnInvulnerability(doomsday);

		boolean oldPvp = helper.getLevel().getServer().isPvpAllowed();
		helper.getLevel().getServer().setPvpAllowed(true);
		try {
			doomsday.hurt(helper.getLevel().damageSources().playerAttack(attacker), 100.0f);
		} finally {
			helper.getLevel().getServer().setPvpAllowed(oldPvp);
		}
		helper.assertTrue(doomsday.isAlive(), "the hit did not kill him");

		awaitShard(helper, doomsday, attacker.getUUID(), 15, () -> {
			TestPlayers.leave(doomsday);
			TestPlayers.leave(attacker);
			helper.succeed();
		});
	}

	private static void awaitShard(GameTestHelper helper, ServerPlayer doomsday, UUID owner,
			int tries, Runnable done) {
		if (tries <= 0) {
			helper.fail("no bound kryptonite shard dropped near the doomsday");
			return;
		}
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
				doomsday.getBoundingBox().inflate(8.0))) {
			ItemStack stack = entity.getItem();
			if (stack.is(DoomsdayItems.KRYPTONITE_SHARD)
					&& owner.equals(KryptoniteShardItem.getOwner(stack))
					&& doomsday.getUUID().equals(KryptoniteShardItem.getTargetDoomsday(stack))) {
				done.run();
				return;
			}
		}
		helper.runAfterDelay(1, () -> awaitShard(helper, doomsday, owner, tries - 1, done));
	}

	/**
	 * {@code KryptoniteShardPickupMixin}: a doomsday can never pick a shard up (even an
	 * unowned one), a shard bound to an owner is denied to everyone else, and only the owner
	 * picks it up.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void kryptoniteShardPickupIsOwnerGated(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		ServerPlayer owner = TestPlayers.join(helper);
		ServerPlayer stranger = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);
		ServerLevel level = helper.getLevel();

		ItemEntity unowned = dropShardAt(level, new ItemStack(DoomsdayItems.KRYPTONITE_SHARD), doomsday);
		unowned.playerTouch(doomsday);
		helper.assertTrue(TestPlayers.count(doomsday, DoomsdayItems.KRYPTONITE_SHARD) == 0
						&& unowned.isAlive(), "a doomsday cannot pick up even an unowned shard");

		ItemEntity bound = dropShardAt(level,
				KryptoniteShardItem.create(owner.getUUID(), doomsday.getUUID()), doomsday);
		bound.playerTouch(doomsday);
		helper.assertTrue(TestPlayers.count(doomsday, DoomsdayItems.KRYPTONITE_SHARD) == 0
						&& bound.isAlive(), "a doomsday cannot pick up a bound shard");
		bound.playerTouch(stranger);
		helper.assertTrue(TestPlayers.count(stranger, DoomsdayItems.KRYPTONITE_SHARD) == 0
						&& bound.isAlive(), "a non-owner stranger cannot pick it up");
		bound.playerTouch(owner);
		helper.assertTrue(TestPlayers.count(owner, DoomsdayItems.KRYPTONITE_SHARD) == 1,
				"the owner picks it up");
		TestPlayers.leave(doomsday);
		TestPlayers.leave(owner);
		TestPlayers.leave(stranger);
		helper.succeed();
	}

	private static ItemEntity dropShardAt(ServerLevel level, ItemStack stack, ServerPlayer at) {
		ItemEntity entity = new ItemEntity(level, at.getX(), at.getY() + 1.0, at.getZ(), stack);
		entity.setPickUpDelay(0);
		level.addFreshEntity(entity);
		return entity;
	}

	/**
	 * The "weakness window": three owned shards consumed by the global tick strip one
	 * adaptation — the stripped type lands again and is barred from being re-learned by the
	 * auto-adapt path.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void threeOwnedShardsStripAnAdaptation(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		ServerPlayer attacker = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);
		TestPlayers.clearSpawnInvulnerability(doomsday);
		DamageSource freeze = helper.getLevel().damageSources().freeze();
		DoomsdayAdaptationController.registerAdaptation(doomsday, DamageTypes.FREEZE, false);
		helper.assertTrue(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
				"adaptation staged");

		ItemStack shards = KryptoniteShardItem.create(attacker.getUUID(), doomsday.getUUID());
		shards.setCount(3);
		attacker.getInventory().add(shards);

		// The module's global tick fires the cleanse every 20 server ticks.
		helper.runAfterDelay(25, () -> {
			helper.assertFalse(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
					"three owned shards strip one adaptation");
			helper.assertTrue(TestPlayers.count(attacker, DoomsdayItems.KRYPTONITE_SHARD) == 0,
					"the three shards are consumed");
			AttributeInstance attack = doomsday.getAttribute(Attributes.ATTACK_DAMAGE);
			helper.assertTrue(attack == null
							|| attack.getModifier(DoomsdayModifiers.DOOMSDAY_ADAPT_DAMAGE) == null,
					"losing the last adaptation removes the damage bonus");

			hit(doomsday, freeze, 5.0f);
			helper.assertTrue(doomsday.isAlive() && doomsday.getHealth() < doomsday.getMaxHealth(),
					"the stripped type deals damage again — the weakness window");
			for (int i = 0; i < 3; i++) {
				hit(doomsday, freeze, 4.0f);
			}
			helper.assertFalse(DoomsdayAdaptationController.hasAdapted(doomsday, freeze),
					"a stripped type is banned from auto re-learning");
			TestPlayers.leave(doomsday);
			TestPlayers.leave(attacker);
			helper.succeed();
		});
	}

	// ─────────────────────────── effect adaptation ───────────────────────────

	/**
	 * {@code LivingEntityEffectMixin} → {@code DoomsdayEffectAdaptationController}: a tracked
	 * effect re-applied ≥200 ticks after its first application is denied and permanently
	 * adapted; untracked effects and non-doomsday players are untouched.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void trackedEffectAdaptsInsideTheRealtimeWindow(GameTestHelper helper) {
		ServerPlayer doomsday = TestPlayers.join(helper);
		ServerPlayer stranger = TestPlayers.join(helper);
		TestHeroes.transform(doomsday, DOOMSDAY);

		helper.assertTrue(doomsday.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0)),
				"first tracked application is allowed");
		helper.assertTrue(doomsday.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0)),
				"untracked effects always apply");

		helper.runAfterDelay(205, () -> {
			helper.assertFalse(doomsday.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0)),
					"re-apply after the 200-tick window adapts and denies");
			helper.assertTrue(DoomsdayEffectAdaptationController.hasAdapted(doomsday, MobEffects.WEAKNESS),
					"weakness is now adapted");
			helper.assertFalse(doomsday.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 0)),
					"adapted effects stay denied");
			helper.assertFalse(DoomsdayEffectAdaptationController.hasAdapted(doomsday, MobEffects.GLOWING),
					"untracked effects never adapt");
			helper.assertTrue(stranger.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0)),
					"a non-doomsday player is untouched");
			TestPlayers.leave(doomsday);
			TestPlayers.leave(stranger);
			helper.succeed();
		});
	}

	// ─────────────────────────── doom grip lifecycle ───────────────────────────

	/**
	 * {@code DoomGripAbility} picks the nearest hostile in an 8-block box, then
	 * {@code DoomGripController} holds NO_AI on the victim and INVULNERABLE on the doomsday;
	 * the {@code onLeave} lifecycle hook releases everything. Actors are teleported to +2000
	 * so no foreign test entity can win the scan.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void doomGripLocksTargetAndReleasesOnLeave(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, DOOMSDAY);
		DoomsdayTierController.setTier(player, 7); // doom grip unlocks at tier 7

		double isoX = player.getX() + 2000.0;
		double isoZ = player.getZ() + 2000.0;
		helper.getLevel().setChunkForced(((int) isoX) >> 4, ((int) isoZ) >> 4, true);
		helper.getLevel().getChunk(BlockPos.containing(isoX, player.getY(), isoZ));
		player.teleportTo(isoX, player.getY(), isoZ);
		Zombie zombie = spawnAhead(helper, player);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			awaitScanSees(helper, player, zombie, 100, () -> {
				gripBody(helper, player, zombie);
			});
		});
	}

	private static void gripBody(GameTestHelper helper, ServerPlayer player, Zombie zombie) {
		AbilityRouter.activate(player, DOOMSDAY_DOOM_GRIP);

		helper.assertTrue(DoomGripController.isGripping(player), "grip session started");
		helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
						.contains(player.getUUID()), "grip holds NO_AI on the victim");
		helper.assertTrue(TestPlayers.lockOwners(player, ControlLockKind.INVULNERABLE)
						.contains(player.getUUID()), "grip holds INVULNERABLE on the doomsday");

		TestPlayers.leave(player);
		helper.assertFalse(DoomGripController.isGripping(player), "leave ends the session");
		helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty()
						&& TestPlayers.lockOwners(player, ControlLockKind.INVULNERABLE).isEmpty(),
				"leave releases every grip lock");
		helper.succeed();
	}

	/**
	 * {@link TestPlayers#awaitVisible} only waits for the entity to enter the UUID index,
	 * but the ability's {@code getEntitiesOfClass} reads the spatial section — a fresh mob
	 * can be index-visible a tick or two before the section sees it. Poll the same query
	 * shape the ability runs before activating so a miss is impossible.
	 */
	private static void awaitScanSees(GameTestHelper helper, ServerPlayer player, LivingEntity victim,
			int tries, Runnable body) {
		AABB box = new AABB(player.position().subtract(8, 8, 8), player.position().add(8, 8, 8));
		if (tries <= 0
				|| helper.getLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e == victim).contains(victim)) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitScanSees(helper, player, victim, tries - 1, body));
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player) {
		Vec3 ahead = player.position().add(player.getViewVector(1f).normalize().scale(2.0));
		// Forced load: a transiently loaded chunk can unload mid-test and drop the
		// zombie from the spatial section the ability's scan reads.
		helper.getLevel().setChunkForced(((int) ahead.x) >> 4, ((int) ahead.z) >> 4, true);
		helper.getLevel().getChunk(BlockPos.containing(ahead.x, player.getY(), ahead.z));
		Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
		zombie.moveTo(ahead.x, player.getY(), ahead.z, 0f, 0f);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		helper.getLevel().addFreshEntity(zombie);
		return zombie;
	}

	// ─────────────────────────── command surface ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void doomsdayTierCommandSetsTierOnTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "doomsday-target");
		TestHeroes.transform(player, DOOMSDAY);
		MinecraftServer server = helper.getLevel().getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				"superheroes doomsday tier doomsday-target 5");

		helper.assertTrue(progress(player).tier() == 5, "target form sets the tier");
		helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 20.0,
				"tier 5 armor set applied, got " + player.getAttributeValue(Attributes.ARMOR));

		server.getCommands().performPrefixedCommand(
				player.createCommandSourceStack().withPermission(4),
				"superheroes doomsday tier 3");
		helper.assertTrue(progress(player).tier() == 3, "self form sets the tier");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void doomsdayTierCommandRejectsNonDoomsday(GameTestHelper helper) {
		ServerPlayer other = TestPlayers.join(helper, "not-doomsday");
		MinecraftServer server = helper.getLevel().getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				"superheroes doomsday tier not-doomsday 6");

		helper.assertTrue(progress(other).tier() == 1, "rejected target keeps tier 1");
		helper.assertTrue(other.getAttributeValue(Attributes.ARMOR) == 0.0,
				"rejected target gets no tier modifiers");
		TestPlayers.leave(other);
		helper.succeed();
	}

	// ─────────────────────────── progress sync ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void tierChangeSyncsProgressToTheClient(GameTestHelper helper) {
		Wire wire = joinAudible(helper, "doomsday-wire");
		TestHeroes.transform(wire.player(), DOOMSDAY);
		drain(wire.channel()); // drop the transform-time sync

		DoomsdayTierController.setTier(wire.player(), 5);

		awaitProgress(helper, wire.channel(), 5, 15, () -> {
			TestPlayers.leave(wire.player());
			helper.succeed();
		});
	}

	// ─────────────────────────── packet capture ───────────────────────────

	/** A joined player whose raw outbound packets stay readable for assertions. */
	private record Wire(ServerPlayer player, EmbeddedChannel channel) {
	}

	private static Wire joinAudible(GameTestHelper helper, String name) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				profile, cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		// Connection.send dispatches writes via eventLoop().execute() when called off the
		// embedded loop; without running pending tasks readOutbound legitimately reads empty.
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static void awaitProgress(GameTestHelper helper, EmbeddedChannel channel,
			int tier, int tries, Runnable done) {
		if (tries <= 0) {
			helper.fail("doomsday_progress payload (tier=" + tier + ") never reached the client");
			return;
		}
		for (Object o : drain(channel)) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof DoomsdayProgressS2CPayload progress
					&& progress.tier() == tier) {
				done.run();
				return;
			}
		}
		helper.runAfterDelay(1, () -> awaitProgress(helper, channel, tier, tries - 1, done));
	}
}
