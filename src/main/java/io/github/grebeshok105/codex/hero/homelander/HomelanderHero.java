package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.mechanic.ability.SharedAbilityIds;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroHudConfig;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.core.hero.LandingImpact;
import io.github.grebeshok105.codex.core.hero.PassiveGlyph;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.mechanic.shockwave.ShockwaveUtil;
import io.github.grebeshok105.codex.core.model.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class HomelanderHero implements Hero {
	public static final ResourceLocation ID = ModId.of("homelander");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/homelander.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0181C2A,
			0xD0080A14,
			0x88FFD27A,
			0x33FFFFFF,
			0xFFFFE07A,
			0xFFB35900,
			0xFFFFD060,
			0x55FFE08A,
			0xFFFFC538,
			0xFF3B1F8A,
			0xFFB58CFF,
			0x55C7A8FF,
			0xFFB58CFF,
			0x55FFD27A,
			0xFFFFC538,
			0xFFFFC538,
			0xFFFFF1B0,
			0x55FFD27A
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.laser_power", HeroHudConfig.EnergyIconType.LIGHTNING, true, "STUNNING ROAR");

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/homelander/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/homelander/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/homelander/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/homelander/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/homelander/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/homelander/knockback_resistance");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 8.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 20.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 100f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 0.5f;
	}

	@Override
	public float getManaMax() {
		return 100f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
			default -> EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				SharedAbilityIds.FLIGHT,
				HomelanderAbilityIds.EYE_LASERS,
				HomelanderAbilityIds.X_RAY,
				HomelanderAbilityIds.IRON_FISTS,
				HomelanderAbilityIds.HAND_CLAP,
				HomelanderAbilityIds.STUNNING_ROAR);
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
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
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
	public void onLanded(ServerPlayer player, LandingImpact impact) {
		float intensity = impact.intensity();
		float scale = 0.30f + intensity * 1.20f;
		double radius = 3.0 + scale * 8.0;
		float damage = 4.0f + scale * 10.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false, true);

		// The whole landing presentation is event-driven now: the client plays
		// homelander.flight.land + the impact effect; the tier still drives scale.
		VfxFx.event(player, HomelanderVfxIds.LANDING, player.position(), player.position(), scale);
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public io.github.grebeshok105.codex.core.hero.ImpactStyle getImpactStyle() {
		return io.github.grebeshok105.codex.core.hero.ImpactStyle.BRUTAL;
	}
	@Override
	public double getImpactPower() {
		return 1.15;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.A;
	}

	@Override
	public boolean isAbilitySuppressedBy(io.github.grebeshok105.codex.core.model.HeroData data,
			ResourceLocation abilityId) {
		// While Iron Fists stance is up, every other ability is locked out.
		return data.isActive(HomelanderAbilityIds.IRON_FISTS) && !HomelanderAbilityIds.IRON_FISTS.equals(abilityId);
	}

	@Override
	public boolean isUraniumWeak() {
		return true;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.HEART, PassiveGlyph.FLAME, PassiveGlyph.FEATHER);
	}

}
