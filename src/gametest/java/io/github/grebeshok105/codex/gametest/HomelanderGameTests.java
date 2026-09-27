package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.effect.FlightController;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.UraniumDefenseController;
import io.github.grebeshok105.codex.hero.HomelanderHero;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.mechanic.ability.SharedAbilityIds;
import io.github.grebeshok105.codex.network.UraniumPressureS2CPayload;
import io.github.grebeshok105.codex.network.UraniumThreatS2CPayload;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * I6a characterization pins for the Homelander player-hero, written against the
 * pre-migration layout. Every assertion describes current behaviour and must keep
 * passing after the files move to {@code hero/homelander/} and
 * {@code mechanic/flight/}.
 *
 * <p>Already pinned elsewhere (not duplicated here): Iron Fists suppression and the
 * aftermath activation blocker ({@code AbilityGateGameTests}), the Homelander/Omni-Man
 * reaction bark both ways ({@code OmnimanGameTests}).
 */
public final class HomelanderGameTests implements FabricGameTest {

	// ---- hero shape --------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void homelanderOwnsItsAbilitiesInSlotOrder(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		helper.assertTrue(data(player).hasHero(), "transform applied");
		List<ResourceLocation> abilities = Heroes.get(HomelanderHero.ID).getAbilities();
		helper.assertTrue(abilities.size() == 6, "homelander owns exactly 6 abilities, got " + abilities.size());
		helper.assertTrue(abilities.get(0).equals(SharedAbilityIds.FLIGHT), "slot 0 is the shared flight toggle");
		helper.assertTrue(abilities.get(1).equals(AbilityIds.EYE_LASERS), "slot 1 is eye_lasers");
		helper.assertTrue(abilities.get(2).equals(AbilityIds.X_RAY), "slot 2 is x_ray");
		helper.assertTrue(abilities.get(3).equals(AbilityIds.IRON_FISTS), "slot 3 is iron_fists");
		helper.assertTrue(abilities.get(4).equals(AbilityIds.HAND_CLAP), "slot 4 is hand_clap");
		helper.assertTrue(abilities.get(5).equals(AbilityIds.STUNNING_ROAR), "slot 5 is stunning_roar");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void passivesApplyOnTransformAndStripOnUntransform(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 20.0,
				"armor = 20, got " + player.getAttributeValue(Attributes.ARMOR));
		helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 8.0,
				"toughness = 8, got " + player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7.0,
				"attack damage = 7, got " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == 40.0,
				"max health = 40, got " + player.getAttributeValue(Attributes.MAX_HEALTH));
		helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1.0,
				"knockback resistance = 1, got " + player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "infinite fire resistance passive");
		helper.assertTrue(player.getEffect(MobEffects.FIRE_RESISTANCE).isInfiniteDuration(),
				"fire resistance is the infinite passive");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0.0,
				"untransform strips armor, got " + player.getAttributeValue(Attributes.ARMOR));
		helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == 20.0,
				"untransform restores max health");
		helper.assertFalse(player.hasEffect(MobEffects.FIRE_RESISTANCE),
				"untransform strips the fire resistance passive");
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ---- milk / madness ----------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void milkBottleRefusesNonHomelander(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MILK_BOTTLE));
		InteractionResultHolder<ItemStack> result =
				ModItems.MILK_BOTTLE.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(result.getResult() == InteractionResult.FAIL,
				"use() refuses a non-homelander");
		ItemStack out = ModItems.MILK_BOTTLE.finishUsingItem(
				player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player);
		helper.assertTrue(out.is(ModItems.MILK_BOTTLE), "finishUsingItem returns the bottle untouched");
		helper.assertFalse(player.hasEffect(ModEffects.MADNESS), "no madness for outsiders");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void milkBottleGrantsMadnessAndEmpties(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		HeroDataStore.update(player, d -> d.withResources(10f, 10f));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MILK_BOTTLE));

		InteractionResultHolder<ItemStack> result =
				ModItems.MILK_BOTTLE.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(result.getResult() == InteractionResult.CONSUME,
				"use() starts drinking for homelander");
		ItemStack out = ModItems.MILK_BOTTLE.finishUsingItem(
				player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player);

		MobEffectInstance madness = player.getEffect(ModEffects.MADNESS);
		helper.assertTrue(madness != null, "drinking milk grants madness");
		helper.assertTrue(madness.getDuration() == 300,
				"madness lasts 300 ticks, got " + madness.getDuration());
		helper.assertTrue(out.is(Items.GLASS_BOTTLE), "the empty glass bottle comes back");
		helper.assertTrue(data(player).energy() == 100f,
				"madness start refills energy, got " + data(player).energy());
		helper.assertTrue(data(player).mana() == 100f,
				"madness start refills mana, got " + data(player).mana());
		MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
		helper.assertTrue(regen != null && regen.getAmplifier() == 4,
				"madness start grants regen 5");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void madnessExpiryTriggersAftermath(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.MADNESS, 600));
		AbilityRouter.activate(player, AbilityIds.X_RAY);
		helper.assertTrue(data(player).isActive(AbilityIds.X_RAY), "x_ray toggles on in madness");
		// The aftermath is an edge detector: at least one tick must see MADNESS present
		// before its removal counts.
		helper.runAfterDelay(2, () -> player.removeEffect(ModEffects.MADNESS));
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(player.hasEffect(ModEffects.MADNESS_AFTERMATH),
					"madness expiry arms the aftermath");
			MobEffectInstance aftermath = player.getEffect(ModEffects.MADNESS_AFTERMATH);
			helper.assertTrue(aftermath.getDuration() <= 200 && aftermath.getDuration() > 190,
					"aftermath lasts 200 ticks, got " + aftermath.getDuration());
			MobEffectInstance resist = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
			helper.assertTrue(resist != null && resist.getAmplifier() == 4,
					"aftermath comes with resistance 5");
			helper.assertFalse(data(player).isActive(AbilityIds.X_RAY),
					"the aftermath deactivates running abilities");
			helper.assertTrue(player.getDeltaMovement().lengthSqr() < 0.01,
					"the aftermath pins player motion, got " + player.getDeltaMovement());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void aftermathEndsInDetonation(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);

		// The sun detonation scorches a wide radius; run it on a private platform far
		// away so the blast can't reach the shared test floor or neighbouring tests.
		BlockPos remoteFeet = player.blockPosition().offset(3000, 0, 3000);
		for (int dx = -13; dx <= 13; dx++) {
			for (int dz = -13; dz <= 13; dz++) {
				helper.getLevel().setBlock(remoteFeet.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		// A FORCED ticket makes the remote chunk entity-ticking — getChunk/setBlock
		// alone only load it, and the effect duration would never decrement.
		helper.getLevel().setChunkForced(remoteFeet.getX() >> 4, remoteFeet.getZ() >> 4, true);
		player.teleportTo(remoteFeet.getX() + 0.5, remoteFeet.getY(), remoteFeet.getZ() + 0.5);
		helper.getLevel().getChunkSource().chunkMap.move(player);
		player.setDeltaMovement(Vec3.ZERO);

		player.addEffect(new MobEffectInstance(ModEffects.MADNESS_AFTERMATH, 25));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));

		helper.runAfterDelay(26, () -> await(helper,
				() -> !player.hasEffect(ModEffects.MADNESS_AFTERMATH), 8, () -> {
				helper.assertTrue(player.isAlive(), "resistance 5 survives the sun detonation");
				boolean scorched = false;
				for (BlockPos pos : BlockPos.betweenClosed(
						remoteFeet.offset(-12, -2, -12), remoteFeet.offset(12, 4, 12))) {
					if (helper.getLevel().getBlockState(pos).is(Blocks.FIRE)) {
						scorched = true;
						break;
					}
				}
			helper.assertTrue(scorched, "the detonation leaves fire in its wake");
				TestPlayers.leave(player);
				helper.succeed();
			}));
	}

	// ---- flight ------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void flightToggleStartsFallFlyingAndStopsCleanly(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		AbilityRouter.activate(player, SharedAbilityIds.FLIGHT);
		helper.assertTrue(data(player).isActive(SharedAbilityIds.FLIGHT), "flight toggles on");
		helper.assertTrue(FlightController.isFlightActive(data(player)), "controller tracks the flight state");
		helper.assertTrue(player.getAbilities().flying, "vanilla flying is on");
		helper.assertTrue(player.isFallFlying(), "the player is put into elytra flight");

		AbilityRouter.activate(player, SharedAbilityIds.FLIGHT);
		helper.assertFalse(data(player).isActive(SharedAbilityIds.FLIGHT), "flight toggles back off");
		helper.assertFalse(FlightController.isFlightActive(data(player)), "flight state cleared");
		helper.assertFalse(player.getAbilities().flying, "vanilla flying off again");
		helper.assertFalse(player.getAbilities().mayfly, "mayfly stripped in survival");
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ---- uranium threat ----------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void uraniumThreatScanMarksAndClears(GameTestHelper helper) {
		Wire homelander = joinAudible(helper, "homelander-under-threat");
		Wire holder = joinAudible(helper, "dagger-holder");
		Wire bystander = joinAudible(helper, "bystander");
		TestHeroes.transform(homelander.player(), HomelanderHero.ID);
		holder.player().getInventory().add(new ItemStack(ModItems.URANIUM_DAGGER));
		drainAll(homelander, holder, bystander);

		List<Object> homelanderPackets = new ArrayList<>();
		List<Object> bystanderPackets = new ArrayList<>();
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(UraniumDefenseController.isUnderUraniumThreat(homelander.player()),
					"the 20-tick scan marks a threatened homelander");
			// Payloads queue on the connection's event loop: poll while accumulating
			// drained packets instead of reading the queue once.
			await(helper, () -> {
				homelanderPackets.addAll(drain(homelander.channel()));
				bystanderPackets.addAll(drain(bystander.channel()));
				return hasThreat(homelanderPackets, true, 1)
						&& hasPressure(bystanderPackets, homelander.player().getUUID());
			}, 15, () -> {
				helper.assertTrue(hasThreat(homelanderPackets, true, 1),
						"homelander is sent threat=true, count=1");
				helper.assertTrue(hasPressure(bystanderPackets, homelander.player().getUUID()),
						"bystanders get the pressured-homelanders broadcast");

				holder.player().getInventory().clearContent();
				homelanderPackets.clear();
				helper.runAfterDelay(30, () -> {
					helper.assertFalse(UraniumDefenseController.isUnderUraniumThreat(homelander.player()),
							"the threat clears once the dagger holder leaves");
					await(helper, () -> {
						homelanderPackets.addAll(drain(homelander.channel()));
						return hasThreat(homelanderPackets, false, 0);
					}, 15, () -> {
						helper.assertTrue(hasThreat(homelanderPackets, false, 0),
								"the cleared scan sends threat=false, count=0");
						TestPlayers.leave(homelander.player());
						TestPlayers.leave(holder.player());
						TestPlayers.leave(bystander.player());
						helper.succeed();
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 90)
	public void uraniumThreatForcesGroundedFlightOff(GameTestHelper helper) {
		ServerPlayer homelander = TestPlayers.join(helper);
		ServerPlayer holder = TestPlayers.join(helper, "dagger-holder");
		TestHeroes.transform(homelander, HomelanderHero.ID);
		holder.getInventory().add(new ItemStack(ModItems.URANIUM_DAGGER));

		helper.runAfterDelay(30, () -> {
			helper.assertTrue(UraniumDefenseController.isUnderUraniumThreat(homelander),
					"precondition: threat established");
			helper.assertFalse(FlightController.isOnCooldown(homelander),
					"precondition: no uranium cooldown yet");
			helper.assertTrue(AbilityRegistry.get(SharedAbilityIds.FLIGHT).canActivate(homelander),
					"quirk pin: threat alone does not gate activation");

			// Mock players can still be airborne when the scan lands; stand the
			// homelander on a placed floor and wait for the physics tick to settle.
			groundOn(helper, homelander);
			await(helper, homelander::onGround, 15, () -> {
				AbilityRouter.activate(homelander, SharedAbilityIds.FLIGHT);
				helper.assertTrue(data(homelander).isActive(SharedAbilityIds.FLIGHT),
						"the activation itself still succeeds");

				helper.runAfterDelay(3, () -> {
					helper.assertFalse(data(homelander).isActive(SharedAbilityIds.FLIGHT),
							"a grounded threatened homelander is forced out of flight");
					helper.assertTrue(FlightController.isOnCooldown(homelander),
							"the forced-off arms a 20-tick cooldown");
					helper.assertFalse(AbilityRegistry.get(SharedAbilityIds.FLIGHT).canActivate(homelander),
							"threat + cooldown is what the activation gate denies");
					TestPlayers.leave(homelander);
					TestPlayers.leave(holder);
					helper.succeed();
				});
			});
		});
	}

	// ---- combat abilities --------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironFistsLungeHitsDashTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		AbilityRouter.activate(player, AbilityIds.IRON_FISTS);
		helper.assertTrue(data(player).isActive(AbilityIds.IRON_FISTS), "iron fists toggles on");
		helper.assertTrue(data(player).energy() == 0f,
				"iron fists empties the energy bar, got " + data(player).energy());
		Zombie zombie = spawnAhead(helper, player);
		TestPlayers.awaitVisible(helper, zombie, () -> {
			float hp = zombie.getHealth();
			player.attack(zombie);
			helper.assertTrue(zombie.getHealth() < hp, "the dash-hit damages the target");
			helper.assertTrue(zombie.hurtMarked, "the target is hurtMarked");
			helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.1,
					"the player lunges at the target");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void handClapKnocksBackAHeadCone(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		Zombie zombie = spawnAhead(helper, player);
		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, AbilityIds.HAND_CLAP);
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"hand clap hits mobs in front");
			helper.assertTrue(zombie.hurtMarked, "clap victim is knocked back");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.HAND_CLAP),
					"hand clap goes on a 240-tick cooldown");
			helper.assertTrue(data(player).energy() == 50f,
					"hand clap costs 50 energy, got " + data(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void stunningRoarDeafensAround(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		Zombie zombie = spawnAhead(helper, player);
		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, AbilityIds.STUNNING_ROAR);
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
					"the roar damages mobs in radius 12");
			MobEffectInstance darkness = zombie.getEffect(MobEffects.DARKNESS);
			helper.assertTrue(darkness != null && darkness.getDuration() == 80,
					"the roar applies 80 ticks of darkness");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, AbilityIds.STUNNING_ROAR),
					"the roar goes on a 160-tick cooldown");
			helper.assertTrue(data(player).energy() == 70f,
					"the roar costs 30 energy, got " + data(player).energy());
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void xRayHighlightsNearbyHostiles(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		Zombie zombie = spawnAhead(helper, player);
		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, AbilityIds.X_RAY);
			helper.assertTrue(data(player).isActive(AbilityIds.X_RAY), "x_ray toggles on");
			helper.runAfterDelay(4, () -> {
				helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING),
						"hostiles within 32 blocks glow");
				AbilityRouter.deactivate(player, AbilityIds.X_RAY);
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void eyeLasersDrainEnergyWhileActive(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		AbilityRouter.activate(player, AbilityIds.EYE_LASERS);
		helper.assertTrue(data(player).isActive(AbilityIds.EYE_LASERS), "eye lasers toggle on");
		float afterActivate = data(player).energy();
		helper.assertTrue(afterActivate == 96f,
				"activation costs 4 energy, got " + afterActivate);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(data(player).energy() < afterActivate,
					"the beam drains 1.5/t while firing, got " + data(player).energy());
			AbilityRouter.deactivate(player, AbilityIds.EYE_LASERS);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---- uranium items -----------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void uraniumDaggerWeakensHeroVictim(GameTestHelper helper) {
		ServerPlayer attacker = TestPlayers.join(helper);
		ServerPlayer victim = TestPlayers.join(helper, "victim");
		TestHeroes.transform(victim, HomelanderHero.ID);
		TestPlayers.clearSpawnInvulnerability(victim);
		float hp = victim.getHealth();

		ItemStack dagger = new ItemStack(ModItems.URANIUM_DAGGER);
		ModItems.URANIUM_DAGGER.hurtEnemy(dagger, victim, attacker);

		MobEffectInstance weakness = victim.getEffect(MobEffects.WEAKNESS);
		helper.assertTrue(weakness != null && weakness.getAmplifier() == 4 && weakness.getDuration() == 200,
				"the dagger inflicts weakness 5 for 200 ticks");
		MobEffectInstance heroWeakness = victim.getEffect(ModEffects.SUPERHERO_WEAKNESS);
		helper.assertTrue(heroWeakness != null && heroWeakness.getDuration() == 200,
				"the dagger inflicts superhero weakness for 200 ticks");
		helper.assertTrue(victim.getHealth() < hp, "the hit lands magic damage on the victim");
		// tryConsume pulls 200 through the victim's bound pool synchronously inside
		// hurtEnemy — energy pays first and mana covers the deficit, so a full
		// 100/100 victim ends at 0/0. The sum stays correct if the maxes change.
		helper.assertTrue(data(victim).energy() + data(victim).mana() < 200f,
				"the dagger drains 200 across the victim's pools, got energy=" + data(victim).energy()
						+ " mana=" + data(victim).mana());
		TestPlayers.leave(attacker);
		TestPlayers.leave(victim);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void uraniumOffhandGrantsKbResistWhileHeld(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.getInventory().offhand.set(0, new ItemStack(ModItems.URANIUM_ISOTOPE));
		helper.runAfterDelay(3, () -> {
			AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			helper.assertTrue(attr != null && attr.getModifier(ModId.of("uranium_offhand_kb")) != null,
					"holding the isotope adds uranium_offhand_kb");
			player.getInventory().offhand.set(0, ItemStack.EMPTY);
			helper.runAfterDelay(2, () -> {
				AttributeInstance after = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
				helper.assertTrue(after == null || after.getModifier(ModId.of("uranium_offhand_kb")) == null,
						"the modifier drops with the isotope");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 340)
	public void uraniumOffhandRadiationStacksToHunger(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.getInventory().offhand.set(0, new ItemStack(ModItems.URANIUM_ISOTOPE));
		helper.runAfterDelay(305, () -> {
			helper.assertTrue(player.hasEffect(MobEffects.HUNGER),
					"300 ticks of offhand radiation stack to hunger");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---- regen / madness flight --------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void regenEngagesBelowHalfHealth(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		player.setHealth(10f);
		helper.runAfterDelay(25, () -> {
			MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(regen != null && regen.getAmplifier() == 1,
					"below half health homelander holds regen 2");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void madnessFlightChewsTerrainAhead(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, HomelanderHero.ID);
		player.addEffect(new MobEffectInstance(ModEffects.MADNESS, 400));
		AbilityRouter.activate(player, SharedAbilityIds.FLIGHT);
		helper.assertTrue(data(player).isActive(SharedAbilityIds.FLIGHT), "madness flight is active");
		player.setYRot(0f);
		player.setXRot(0f);
		player.setDeltaMovement(Vec3.ZERO);
		BlockPos feet = player.blockPosition();
		List<BlockPos> wall = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = 2; dz <= 4; dz++) {
				for (int dy = 0; dy <= 2; dy++) {
					BlockPos pos = feet.offset(dx, dy, dz);
					helper.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
					wall.add(pos);
				}
			}
		}
		helper.runAfterDelay(40, () -> {
			boolean broke = false;
			for (BlockPos pos : wall) {
				if (!helper.getLevel().getBlockState(pos).is(Blocks.DIRT)) {
					broke = true;
					break;
				}
			}
			helper.assertTrue(broke, "madness flight chews blocks ahead of the player");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ---- helpers ------------------------------------------------------------

	private static HeroData data(ServerPlayer player) {
		return HeroDataStore.get(player);
	}

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
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	/** Retries {@code ready} once per tick until it holds; drains may accumulate packets. */
	private static void await(GameTestHelper helper, BooleanSupplier ready, int tries, Runnable done) {
		if (ready.getAsBoolean()) {
			done.run();
			return;
		}
		helper.assertTrue(tries > 0, "condition never became true");
		helper.runAfterDelay(1, () -> await(helper, ready, tries - 1, done));
	}

	/** Plants a stone floor under the player and drops them onto it. */
	private static void groundOn(GameTestHelper helper, ServerPlayer player) {
		BlockPos feet = player.blockPosition();
		helper.getLevel().setBlock(feet.below(), Blocks.STONE.defaultBlockState(), 3);
		player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, player.getYRot(), player.getXRot());
		player.setDeltaMovement(Vec3.ZERO);
		player.setOnGround(true);
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static void drainAll(Wire... wires) {
		for (Wire wire : wires) {
			drain(wire.channel());
		}
	}

	private static boolean hasPressure(List<Object> packets, UUID homelanderId) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof UraniumPressureS2CPayload pressure
					&& pressure.pressuredHomelanders().contains(homelanderId)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasThreat(List<Object> packets, boolean self, int count) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof UraniumThreatS2CPayload threat
					&& threat.self() == self && threat.sourceCount() == count) {
				return true;
			}
		}
		return false;
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player) {
		Vec3 ahead = player.position().add(player.getViewVector(1f).normalize().scale(2.0));
		helper.getLevel().getChunk(BlockPos.containing(ahead.x, player.getY(), ahead.z));
		Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
		zombie.moveTo(ahead.x, player.getY(), ahead.z, 0f, 0f);
		helper.getLevel().addFreshEntity(zombie);
		return zombie;
	}
}
