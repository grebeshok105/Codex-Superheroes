package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.ironman.IronManNanoForm;
import io.github.grebeshok105.codex.hero.ironman.IronManSuitVariant;
import io.github.grebeshok105.codex.hero.ironman.IronManAttachments;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.core.hero.PassiveGlyph;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.effect.FlightController;
import io.github.grebeshok105.codex.flight.FlightAbilityState;
import io.github.grebeshok105.codex.flight.FlightMode;
import io.github.grebeshok105.codex.flight.FlightProfiles;
import io.github.grebeshok105.codex.flight.FlightTuning;
import io.github.grebeshok105.codex.hero.ironman.net.NanoFormS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.ReactorStateS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.SuitVariantS2CPayload;
import io.github.grebeshok105.codex.mechanic.impact.CombatImpactEngine;
import io.github.grebeshok105.codex.mechanic.impact.ImpactProfile;
import io.github.grebeshok105.codex.mechanic.impact.ImpactTier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

/**
 * Stage I6b characterization: pins Iron Man's CURRENT observable server behavior on the
 * pre-move layout — the ability slot order, static hero contract (binding/style/threat/glyphs,
 * the unibeam 100-energy reserve), the six passive attribute modifiers, the transformation
 * item, the registry rows that move into the hero module, the flight-mode machine
 * (toggle on/off, supersonic-over-iron-man precedence, the 100-energy floor tick), the
 * FlightProfiles tuning table, the nano-form/suit attachments and ability cycles, and the
 * CombatImpactEngine nano-hammer branch that Phase 2 reroutes through Hero.modifyImpact.
 * Ids are literals because the constants move into the hero module with it.
 */
public final class IronManGameTests implements FabricGameTest {
	private static final ResourceLocation IRON_MAN = ModId.of("iron_man");
	private static final ResourceLocation IRON_MAN_FLIGHT = ModId.of("iron_man_flight");
	private static final ResourceLocation SUPERSONIC = ModId.of("supersonic");
	private static final ResourceLocation REPULSOR = ModId.of("repulsor");
	private static final ResourceLocation UNIBEAM = ModId.of("unibeam");
	private static final ResourceLocation SMART_MISSILE = ModId.of("iron_man_smart_missile");
	private static final ResourceLocation NANO_FORM = ModId.of("iron_man_nano_form");
	private static final ResourceLocation SUIT_SWITCH = ModId.of("iron_man_suit_switch");
	private static final ResourceLocation IRON_LEGION = ModId.of("iron_man_legion");
	private static final ResourceLocation NORMAL_FLIGHT = ModId.of("flight");

	private static final double EPS = 1.0e-6;

	// ─────────────────────────── registration + static contract ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManOwnsItsEightAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero hero = Heroes.get(IRON_MAN);
		helper.assertTrue(hero != null, "the iron_man hero is registered");
		List<ResourceLocation> expected = List.of(
				IRON_MAN_FLIGHT, SUPERSONIC, REPULSOR, UNIBEAM,
				SMART_MISSILE, NANO_FORM, SUIT_SWITCH, IRON_LEGION);
		helper.assertTrue(expected.equals(hero.getAbilities()),
				"iron_man ability slot order: expected " + expected + " got " + hero.getAbilities());
		for (ResourceLocation id : expected) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " is registered in AbilityRegistry");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManHeroStaticsStayPinned(GameTestHelper helper) {
		Hero hero = Heroes.get(IRON_MAN);
		helper.assertTrue(hero != null, "the iron_man hero is registered");
		helper.assertTrue(hero.getEnergyMax() == 1000f && hero.getEnergyRegenPerTick() == 3.0f
						&& hero.getManaMax() == 0f,
				"energy 1000 / regen 3 / mana 0");
		for (ResourceLocation id : hero.getAbilities()) {
			helper.assertTrue(hero.getDefaultBinding(id) == ResourceKind.ENERGY,
					id + " binds ENERGY");
		}
		helper.assertTrue(hero.getImpactStyle() == ImpactStyle.ENERGY, "impact style ENERGY");
		helper.assertTrue(CombatImpactEngine.styleOf(IRON_MAN) == ImpactStyle.ENERGY,
				"CombatImpactEngine resolves ENERGY");
		helper.assertTrue(CombatImpactEngine.heroPowerOf(IRON_MAN) == hero.getImpactPower(),
				"impact power routes through the hero hook");
		helper.assertTrue(hero.getThreatClass() == JarvisThreatClass.B, "threat class B");
		helper.assertTrue(hero.getPassiveGlyphs().equals(
						List.of(PassiveGlyph.SHIELD, PassiveGlyph.FEATHER, PassiveGlyph.REACTOR)),
				"passive glyphs SHIELD/FEATHER/REACTOR");
		// The unibeam keeps a 100-energy floor: every other ENERGY ability reports 100, unibeam 0.
		helper.assertTrue(hero.getEnergyReserveFor(UNIBEAM, ResourceKind.ENERGY) == 0f,
				"unibeam itself has no reserve");
		helper.assertTrue(hero.getEnergyReserveFor(REPULSOR, ResourceKind.ENERGY) == 100f,
				"non-unibeam abilities reserve the unibeam floor");
		helper.assertTrue(hero.getEnergyReserveFor(REPULSOR, ResourceKind.MANA) == 0f,
				"the reserve is ENERGY-only");

		ServerPlayer player = TestPlayers.join(helper, "i6-static");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(hero.cancelsFallDamage(player), "iron man cancels fall damage");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManPassivesApplySixModifiers(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "i6-pasv");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			assertModifier(helper, player, Attributes.ARMOR, "modifiers/iron_man/armor",
					22.0, AttributeModifier.Operation.ADD_VALUE);
			assertModifier(helper, player, Attributes.ARMOR_TOUGHNESS, "modifiers/iron_man/toughness",
					6.0, AttributeModifier.Operation.ADD_VALUE);
			assertModifier(helper, player, Attributes.ATTACK_DAMAGE, "modifiers/iron_man/damage",
					7.0, AttributeModifier.Operation.ADD_VALUE);
			assertModifier(helper, player, Attributes.MOVEMENT_SPEED, "modifiers/iron_man/speed",
					0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			assertModifier(helper, player, Attributes.MAX_HEALTH, "modifiers/iron_man/max_health",
					10.0, AttributeModifier.Operation.ADD_VALUE);
			assertModifier(helper, player, Attributes.KNOCKBACK_RESISTANCE, "modifiers/iron_man/knockback_resistance",
					0.6, AttributeModifier.Operation.ADD_VALUE);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── items + registry rows ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManRegistryRowsExist(GameTestHelper helper) {
		helper.assertTrue(item("iron_man_suit") != null, "iron_man_suit registered");
		helper.assertTrue(item("iron_man_reactor") != null, "iron_man_reactor registered");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getOptional(ModId.of("smart_missile")).isPresent(),
				"smart_missile entity registered");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getOptional(ModId.of("iron_legion_drone")).isPresent(),
				"iron_legion_drone entity registered");
		helper.assertTrue(BuiltInRegistries.SOUND_EVENT.getOptional(ModId.of("ironman.jarvis_detect")).isPresent()
						&& BuiltInRegistries.SOUND_EVENT.getOptional(ModId.of("unibeam.beam")).isPresent(),
				"jarvis/unibeam sounds registered");
		helper.assertTrue(NanoFormS2CPayload.TYPE.id().equals(ModId.of("nano_form"))
						&& SuitVariantS2CPayload.TYPE.id().equals(ModId.of("suit_variant"))
						&& ReactorStateS2CPayload.TYPE.id().equals(ModId.of("reactor_state")),
				"iron man S2C payload ids are byte-identical");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManSuitItemTransformsAndUntransforms(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "i6-suit");
		Item suit = item("iron_man_suit");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(suit));
		InteractionResult result = useHeld(helper, player);
		helper.assertTrue(result == InteractionResult.CONSUME, "suit use transforms");
		helper.assertTrue(IRON_MAN.equals(HeroDataStore.get(player).heroId()), "player is iron man now");
		// untransform shares the 20-tick transform cooldown — wait it out.
		helper.runAfterDelay(22, () -> {
			player.setShiftKeyDown(true);
			helper.assertTrue(useHeld(helper, player) == InteractionResult.CONSUME,
					"shift-use untransforms");
			helper.assertTrue(!HeroDataStore.get(player).hasHero(), "hero cleared");
			player.setShiftKeyDown(false);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── flight modes (the plan-mandated pin) ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManFlightToggleDrivesVanillaFlight(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "i6-fly");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			AbilityRouter.activate(player, IRON_MAN_FLIGHT);
			HeroData data = HeroDataStore.get(player);
			helper.assertTrue(data.isActive(IRON_MAN_FLIGHT), "flight ability toggled on");
			helper.assertTrue(FlightController.isFlightActive(data), "flight controller sees it");
			helper.assertTrue(FlightController.activeMode(data) == FlightMode.IRON_MAN,
					"mode is IRON_MAN");
			helper.assertTrue(player.getAbilities().mayfly && player.getAbilities().flying,
					"vanilla flight granted");
			helper.assertTrue(player.isFallFlying(), "fall-flying flag set");

			AbilityRouter.activate(player, IRON_MAN_FLIGHT); // toggle off
			data = HeroDataStore.get(player);
			helper.assertTrue(!data.isActive(IRON_MAN_FLIGHT), "second use toggles off");
			helper.assertTrue(!FlightController.isFlightActive(data), "flight cleared");
			helper.assertTrue(!player.getAbilities().mayfly && !player.getAbilities().flying,
					"vanilla flight revoked for a survival player");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void supersonicOutranksAndFallsBackToIronManFlight(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "i6-sonic");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			AbilityRouter.activate(player, IRON_MAN_FLIGHT);
			AbilityRouter.activate(player, SUPERSONIC);
			HeroData data = HeroDataStore.get(player);
			helper.assertTrue(data.isActive(IRON_MAN_FLIGHT) && data.isActive(SUPERSONIC),
					"both toggles stay on");
			helper.assertTrue(FlightController.activeMode(data) == FlightMode.SUPERSONIC,
					"supersonic wins while both run");

			AbilityRouter.activate(player, SUPERSONIC); // supersonic off, iron man still on
			data = HeroDataStore.get(player);
			helper.assertTrue(!data.isActive(SUPERSONIC) && data.isActive(IRON_MAN_FLIGHT),
					"iron man flight remains toggled");
			helper.assertTrue(FlightController.activeMode(data) == FlightMode.IRON_MAN,
					"mode falls back to IRON_MAN");
			helper.assertTrue(player.getAbilities().flying, "still flying");

			AbilityRouter.activate(player, IRON_MAN_FLIGHT); // last one off
			data = HeroDataStore.get(player);
			helper.assertTrue(FlightController.activeMode(data) == null
							&& !player.getAbilities().flying, "flight fully off");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManFlightTickHoldsTheHundredEnergyFloor(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "i6-floor");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			AbilityRouter.activate(player, IRON_MAN_FLIGHT);
			HeroDataStore.update(player, d -> d.withEnergy(10f));
			FlightController.tickPlayer(player);
			helper.assertTrue(HeroDataStore.get(player).energy() >= 100f,
					"iron-man flight tick lifts energy to the 100 floor");
			AbilityRouter.deactivate(player, IRON_MAN_FLIGHT);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void flightProfilesPinTheTuningTable(GameTestHelper helper) {
		FlightTuning ironMan = FlightProfiles.tuning(FlightMode.IRON_MAN, 1000f, 1000f, false);
		assertTuning(helper, ironMan, 1.7, 0.8925, 0.136, false,
				"full-energy iron man = base * 0.85");
		FlightTuning supersonic = FlightProfiles.tuning(FlightMode.SUPERSONIC, 1000f, 1000f, false);
		assertTuning(helper, supersonic, 6.12, 3.213, 0.4896, true,
				"supersonic = base * 0.85 * 3.6, forces forward");
		FlightTuning normal = FlightProfiles.tuning(FlightMode.NORMAL, 1000f, 1000f, false);
		assertTuning(helper, normal, 2.9, 1.5225, 0.232, false, "normal = base * 1.45");
		FlightTuning half = FlightProfiles.tuning(FlightMode.IRON_MAN, 500f, 1000f, false);
		close(helper, half.maxHorizontalSpeed(), 1.275, "half energy scales by 0.75 * 0.85");
		FlightTuning mad = FlightProfiles.tuning(FlightMode.IRON_MAN, 1000f, 1000f, true);
		close(helper, mad.maxHorizontalSpeed(), 2.55, "madness multiplies by 1.5 instead of the normal bonus");
		FlightTuning noCap = FlightProfiles.tuning(FlightMode.IRON_MAN, 0f, 0f, false);
		close(helper, noCap.maxHorizontalSpeed(), 1.7, "zero energy max means full multiplier");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void flightAbilityStatePinsTheModePriority(GameTestHelper helper) {
		helper.assertTrue(FlightController.isFlightAbility(IRON_MAN_FLIGHT)
						&& FlightController.isFlightAbility(SUPERSONIC)
						&& FlightController.isFlightAbility(NORMAL_FLIGHT)
						&& !FlightController.isFlightAbility(REPULSOR),
				"the three flight ids and nothing else");
		ServerPlayer player = TestPlayers.join(helper, "i6-prio");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			HeroDataStore.update(player, d -> d.withActive(IRON_MAN_FLIGHT, true));
			helper.assertTrue(FlightAbilityState.activeMode(HeroDataStore.get(player)) == FlightMode.IRON_MAN,
					"iron_man_flight alone maps to IRON_MAN");
			HeroDataStore.update(player, d -> d.withActive(NORMAL_FLIGHT, true));
			helper.assertTrue(FlightAbilityState.activeMode(HeroDataStore.get(player)) == FlightMode.IRON_MAN,
					"iron_man_flight outranks plain flight");
			HeroDataStore.update(player, d -> d.withActive(SUPERSONIC, true));
			HeroData data = HeroDataStore.get(player);
			helper.assertTrue(FlightAbilityState.activeMode(data) == FlightMode.SUPERSONIC,
					"supersonic outranks both");
			helper.assertTrue(FlightAbilityState.activeModeExcept(data, SUPERSONIC) == FlightMode.IRON_MAN,
					"removing supersonic falls back to iron man");
			helper.assertTrue(FlightAbilityState.activeModeExcept(data, IRON_MAN_FLIGHT) == FlightMode.SUPERSONIC,
					"removing iron man keeps supersonic");
			HeroDataStore.update(player, d -> d.withActive(SUPERSONIC, false)
					.withActive(IRON_MAN_FLIGHT, false));
			helper.assertTrue(FlightAbilityState.activeMode(HeroDataStore.get(player)) == FlightMode.NORMAL,
					"plain flight alone maps to NORMAL");
			HeroDataStore.update(player, d -> d.withActive(NORMAL_FLIGHT, false));
			helper.assertTrue(FlightAbilityState.activeMode(HeroDataStore.get(player)) == null,
					"nothing active -> no mode");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── nano form + suit variant ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void nanoFormEnumAndAttachmentPin(GameTestHelper helper) {
		helper.assertTrue(IronManNanoForm.NONE.next() == IronManNanoForm.BLADE
						&& IronManNanoForm.BLADE.next() == IronManNanoForm.HAMMER
						&& IronManNanoForm.HAMMER.next() == IronManNanoForm.SHIELD
						&& IronManNanoForm.SHIELD.next() == IronManNanoForm.NONE,
				"forms cycle blade -> hammer -> shield -> off");
		helper.assertTrue(IronManNanoForm.byIndex(0) == IronManNanoForm.NONE
						&& IronManNanoForm.byIndex(1) == IronManNanoForm.BLADE
						&& IronManNanoForm.byIndex(2) == IronManNanoForm.HAMMER
						&& IronManNanoForm.byIndex(3) == IronManNanoForm.SHIELD
						&& IronManNanoForm.byIndex(4) == IronManNanoForm.NONE,
				"index mapping 0-3, out of range -> NONE");
		helper.assertTrue(IronManNanoForm.HAMMER.index() == 2, "the hammer index the impact branch reads");

		ServerPlayer player = TestPlayers.join(helper, "i6-nano");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.NANO_FORM) == 0,
					"fresh iron man has nano form 0");
			AbilityRouter.activate(player, NANO_FORM);
			helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.NANO_FORM) == 1,
					"first use arms the blade");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, NANO_FORM),
					"the 15-tick cooldown arms");
			AbilityRouter.activate(player, NANO_FORM);
			helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.NANO_FORM) == 1,
					"the cooldown blocks an immediate re-cycle");
			helper.runAfterDelay(16, () -> {
				AbilityRouter.activate(player, NANO_FORM);
				helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.NANO_FORM) == 2,
						"after the cooldown the form cycles to the hammer");
				TestPlayers.leave(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void suitSwitchCyclesVariantsWithCooldown(GameTestHelper helper) {
		helper.assertTrue(IronManSuitVariant.count() == 6, "six suit variants");
		helper.assertTrue(IronManSuitVariant.nextIndex(0) == 1 && IronManSuitVariant.nextIndex(5) == 0,
				"the switch wraps at the last variant");
		ServerPlayer player = TestPlayers.join(helper, "i6-suitv");
		TestHeroes.transform(player, IRON_MAN);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.SUIT_VARIANT) == 0,
					"fresh iron man wears variant 0");
			AbilityRouter.activate(player, SUIT_SWITCH);
			helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.SUIT_VARIANT) == 1,
					"the switch advances to variant 1 (Mark 85)");
			helper.assertTrue("Mark 85".equals(IronManSuitVariant.get(1).name()),
					"variant 1 is Mark 85");
			helper.assertTrue(AbilityCooldowns.isOnCooldown(player, SUIT_SWITCH),
					"the 40-tick cooldown arms");
			AbilityRouter.activate(player, SUIT_SWITCH);
			helper.assertTrue(player.getAttachedOrCreate(IronManAttachments.SUIT_VARIANT) == 1,
					"the cooldown blocks an immediate re-cycle");
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── nano-hammer impact branch ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void nanoHammerBoostsTheImpactProfile(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "i6-ham");
		TestHeroes.transform(player, IRON_MAN);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 2);
		helper.runAfterDelay(2, () -> {
			ImpactProfile base = CombatImpactEngine.profileFor(player, IRON_MAN, zombie, 0);
			ImpactProfile thanosBase = CombatImpactEngine.profileFor(player, ModId.of("thanos"), zombie, 0);
			helper.assertTrue(base.tier() == ImpactTier.TIER_1 && base.heroId().equals(IRON_MAN),
					"zero held ticks is TIER_1 for iron man");
			helper.assertTrue(base.launchPower() == 0.0 && base.debrisIntensity() == 0f,
					"a tier-1 punch has no ballistic launch or debris");

			player.setAttached(IronManAttachments.NANO_FORM, IronManNanoForm.BLADE.index());
			ImpactProfile blade = CombatImpactEngine.profileFor(player, IRON_MAN, zombie, 0);
			close(helper, blade.knockback(), base.knockback(), "the blade does not touch impact stats");

			player.setAttached(IronManAttachments.NANO_FORM, IronManNanoForm.HAMMER.index());
			ImpactProfile hammer = CombatImpactEngine.profileFor(player, IRON_MAN, zombie, 0);
			close(helper, hammer.knockback(), base.knockback() * 3.6, "hammer knockback x3.6");
			close(helper, hammer.upwardKnockback(), Math.min(0.95, base.upwardKnockback() + 0.3),
					"hammer upward +0.3 capped at 0.95");
			close(helper, hammer.launchPower(), Math.max(base.launchPower() * 2.6, 340.0),
					"hammer forces launch power 340 even on a tier-1 hit");
			close(helper, hammer.shakeRadius(), Math.max(base.shakeRadius(), 26.0), "hammer shake radius >= 26");
			close(helper, hammer.shakeIntensity(), Math.max(base.shakeIntensity(), 1.4f),
					"hammer shake intensity >= 1.4");
			close(helper, hammer.debrisIntensity(), Math.max(base.debrisIntensity(), 0.9f),
					"hammer debris >= 0.9");
			helper.assertTrue(hammer.damage() == base.damage(),
					"hammer damage is unchanged — it only throws bodies");

			// The branch is hero-gated: the same attachment under a different hero id does nothing.
			ImpactProfile foreign = CombatImpactEngine.profileFor(player, ModId.of("thanos"), zombie, 0);
			helper.assertTrue(foreign.knockback() == thanosBase.knockback()
							&& foreign.launchPower() == thanosBase.launchPower(),
					"the nano attachment only applies to iron man");
			zombie.discard();
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	// ─────────────────────────── payload codecs ───────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironManPayloadCodecsRoundTrip(GameTestHelper helper) {
		UUID id = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
		ByteBuf buf = Unpooled.buffer();
		try {
			NanoFormS2CPayload nano = new NanoFormS2CPayload(id, 2);
			NanoFormS2CPayload.STREAM_CODEC.encode(buf, nano);
			NanoFormS2CPayload nanoDec = NanoFormS2CPayload.STREAM_CODEC.decode(buf);
			helper.assertTrue(nanoDec.playerId().equals(id) && nanoDec.form() == 2,
					"nano_form codec preserves playerId + form");

			buf.clear();
			SuitVariantS2CPayload suit = new SuitVariantS2CPayload(id, 5);
			SuitVariantS2CPayload.STREAM_CODEC.encode(buf, suit);
			SuitVariantS2CPayload suitDec = SuitVariantS2CPayload.STREAM_CODEC.decode(buf);
			helper.assertTrue(suitDec.playerId().equals(id) && suitDec.variant() == 5,
					"suit_variant codec preserves playerId + variant");

			buf.clear();
			ReactorStateS2CPayload reactor = new ReactorStateS2CPayload(true, 17, 40, false);
			ReactorStateS2CPayload.STREAM_CODEC.encode(buf, reactor);
			ReactorStateS2CPayload reactorDec = ReactorStateS2CPayload.STREAM_CODEC.decode(buf);
			helper.assertTrue(reactorDec.active() && reactorDec.progressTicks() == 17
							&& reactorDec.totalTicks() == 40 && !reactorDec.hasStock(),
					"reactor_state codec preserves all four fields");
		} finally {
			buf.release();
		}
		helper.succeed();
	}

	// ─────────────────────────── helpers ───────────────────────────

	private static void assertModifier(GameTestHelper helper, ServerPlayer player,
			Holder<Attribute> attribute,
			String modifierId, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		helper.assertTrue(instance != null, "attribute " + attribute + " exists");
		AttributeModifier modifier = instance == null ? null : instance.getModifier(ModId.of(modifierId));
		helper.assertTrue(modifier != null, modifierId + " applied while transformed");
		helper.assertTrue(modifier != null && Math.abs(modifier.amount() - amount) < 1.0e-6
						&& modifier.operation() == operation,
				modifierId + " amount " + amount + " " + operation);
	}

	private static void assertTuning(GameTestHelper helper, FlightTuning tuning,
			double maxH, double maxV, double accel, boolean forcesForward, String label) {
		close(helper, tuning.maxHorizontalSpeed(), maxH, label + " maxHorizontalSpeed");
		close(helper, tuning.maxVerticalSpeed(), maxV, label + " maxVerticalSpeed");
		close(helper, tuning.acceleration(), accel, label + " acceleration");
		close(helper, tuning.horizontalFriction(), 0.84, label + " horizontalFriction");
		close(helper, tuning.verticalFriction(), 0.78, label + " verticalFriction");
		helper.assertTrue(tuning.forcesForward() == forcesForward, label + " forcesForward");
	}

	private static void close(GameTestHelper helper, double actual, double expected, String label) {
		helper.assertTrue(Math.abs(actual - expected) < EPS,
				label + " — expected " + expected + " got " + actual);
	}

	private static Item item(String path) {
		Item item = BuiltInRegistries.ITEM.get(ModId.of(path));
		if (item == Items.AIR) {
			throw new IllegalStateException("unregistered item " + path);
		}
		return item;
	}

	private static InteractionResult useHeld(GameTestHelper helper, ServerPlayer player) {
		return player.getItemInHand(InteractionHand.MAIN_HAND).getItem()
				.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
	}
}
