package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.effect.ThanosGauntletStateController;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class ThanosHero implements Hero {
	public static final ResourceLocation ID = ModId.of("thanos");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/thanos.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0140828,
			0xD0050210,
			0x88B44CFF,
			0x33FFD040,
			0xFFD58CFF,
			0xFF3A1668,
			0xFFB44CFF,
			0x55D58CFF,
			0xFFB44CFF,
			0xFF1A0608,
			0xFFFFAA40,
			0x55FFCC80,
			0xFFFFAA40,
			0x55B44CFF,
			0xFFFFD040,
			0xFFFFD040,
			0xFFFFFFFF,
			0x55D58CFF
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.cosmic_power", HeroHudConfig.EnergyIconType.COSMIC, true, "SNAP");

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/thanos/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/thanos/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/thanos/damage");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/thanos/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/thanos/knockback_resistance");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/thanos/speed");
	private static final ResourceLocation REACH_ID = ModId.of("modifiers/thanos/entity_reach");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/thanos/step_height");
	private static final ResourceLocation JUMP_ID = ModId.of("modifiers/thanos/jump_strength");
	private static final ResourceLocation SCALE_ID = ModId.of("modifiers/thanos/scale");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 10.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, HP_ID, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, JUMP_ID, 0.4, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.SCALE, SCALE_ID, 0.25, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 400f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 2.0f;
	}

	@Override
	public float getManaMax() {
		return 0f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.7f, 1.6f).withEyeHeight(1.35f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.7f, 0.7f).withEyeHeight(0.45f);
			default -> EntityDimensions.scalable(0.75f, 2.1f).withEyeHeight(1.9f);
		};
	}

	private static final Map<ResourceLocation, InfinityStoneType> ABILITY_STONE = Map.of(
			AbilityIds.THANOS_COSMIC_SLAM, InfinityStoneType.POWER,
			AbilityIds.THANOS_REALITY_TEAR, InfinityStoneType.REALITY,
			AbilityIds.THANOS_MIND_PULSE, InfinityStoneType.MIND,
			AbilityIds.THANOS_TIME_REWIND, InfinityStoneType.TIME,
			AbilityIds.THANOS_SPACE_PORTAL, InfinityStoneType.SPACE,
			AbilityIds.THANOS_SOUL_PULSE, InfinityStoneType.SOUL
	);

	public static InfinityStoneType getRequiredStoneFor(ResourceLocation abilityId) {
		return ABILITY_STONE.get(abilityId);
	}

	public static boolean isSnapAbility(ResourceLocation abilityId) {
		return AbilityIds.THANOS_SNAP.equals(abilityId);
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.THANOS_COSMIC_SLAM,
				AbilityIds.THANOS_REALITY_TEAR,
				AbilityIds.THANOS_MIND_PULSE,
				AbilityIds.THANOS_TIME_REWIND,
				AbilityIds.THANOS_SPACE_PORTAL,
				AbilityIds.THANOS_SOUL_PULSE,
				AbilityIds.THANOS_SNAP
		);
	}

	public boolean isAbilityUnlocked(Player player, ResourceLocation abilityId) {
		if (!(player instanceof ServerPlayer sp)) return true;
		EnumSet<InfinityStoneType> stones = ThanosGauntletStateController.getCurrentStones(sp);
		if (AbilityIds.THANOS_SNAP.equals(abilityId)) {
			return stones.size() >= 6;
		}
		InfinityStoneType req = ABILITY_STONE.get(abilityId);
		if (req == null) return true;
		return stones.contains(req);
	}

	public static void notifyMissingStone(ServerPlayer player, ResourceLocation abilityId) {
		if (AbilityIds.THANOS_SNAP.equals(abilityId)) {
			int have = ThanosGauntletStateController.getCurrentStones(player).size();
			player.displayClientMessage(
					Component.translatable("ability.superheroes.thanos_snap.gate_failed", have, 6), true);
			return;
		}
		InfinityStoneType req = ABILITY_STONE.get(abilityId);
		if (req == null) return;
		player.displayClientMessage(
				Component.translatable("ability.superheroes.thanos.gate_failed",
						Component.translatable(req.getStoneNameKey())), true);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public AttributeModifierSet passiveAttributes() {
		return PASSIVES;
	}

	@Override
	public void applyPassives(Player player) {
		PASSIVES.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		HeroAttributes.thanosClearStoneModifiers(player);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return true;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}

	@Override
	public HeroTheme getTheme() {
		return THEME;
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public double getImpactPower() {
		return 1.25;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.S;
	}

	@Override
	public boolean canUseAbility(ServerPlayer player, com.example.superheroes.transform.HeroData data,
			ResourceLocation abilityId) {
		return isAbilityUnlocked(player, abilityId);
	}

	@Override
	public void onAbilityDenied(ServerPlayer player, ResourceLocation abilityId) {
		notifyMissingStone(player, abilityId);
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.SHIELD, PassiveGlyph.STAR, PassiveGlyph.COSMIC);
	}

	@Override
	public boolean canSuperJump() {
		return true;
	}

}
