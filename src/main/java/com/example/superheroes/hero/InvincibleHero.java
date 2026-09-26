package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.ViltrumiteChargeAbility;
import com.example.superheroes.physics.ShockwaveUtil;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class InvincibleHero implements Hero {
	public static final ResourceLocation ID = ModId.of("invincible");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/invincible.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE0081F38,
			0xD0030C1A,
			0x990AA4D8,
			0x44FFE15A,
			0xFFFFE15A,
			0xFF073C68,
			0xFF0AA4D8,
			0x660AD8FF,
			0xFF33CFFF,
			0xFF2B2200,
			0xFFFFD640,
			0x66FFE878,
			0xFFFFD640,
			0x660AA4D8,
			0xFFFFD640,
			0xFFFFD640,
			0xFFFFFFFF,
			0x66FFE15A
	);

	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.viltrumite_power", HeroHudConfig.EnergyIconType.FIST, true, "GUARDIAN'S BREAKER");

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/invincible/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/invincible/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/invincible/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/invincible/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/invincible/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/invincible/knockback_resistance");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/invincible/attack_speed");
	private static final ResourceLocation JUMP_ID = ModId.of("modifiers/invincible/jump_strength");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/invincible/step_height");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 26.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 12.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 40.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, JUMP_ID, 0.3, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 0.5, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 240f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.2f;
	}

	@Override
	public float getManaMax() {
		return 0f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
			default -> EntityDimensions.scalable(0.6f, 1.85f).withEyeHeight(1.65f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.FLIGHT,
				AbilityIds.VILTRUMITE_CHARGE,
				AbilityIds.VILTRUMITE_RECOVERY,
				AbilityIds.GUARDIANS_BREAKER);
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
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		player.removeEffect(MobEffects.REGENERATION);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		if (player instanceof ServerPlayer sp) {
			ViltrumiteChargeAbility.clear(sp);
		}
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
		float scale = 0.25f + intensity * 1.1f;
		double radius = 2.8 + scale * 7.0;
		float damage = 3.0f + scale * 8.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 0.3, cz,
				(int) (16 + radius * 5), radius * 0.35, 0.15, radius * 0.35, 0.12);
		if (impact.tier() == LandingImpact.Tier.STRONG || impact.tier() == LandingImpact.Tier.EPIC) {
			level.sendParticles(ParticleTypes.FLASH, cx, cy + 0.8, cz, 1, 0, 0, 0, 0);
			level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
	}

	@Override
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public com.example.superheroes.physics.ImpactStyle getImpactStyle() {
		return com.example.superheroes.physics.ImpactStyle.BRUTAL;
	}
	@Override
	public double getImpactPower() {
		return 1.22;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.A;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.SHIELD, PassiveGlyph.FIST, PassiveGlyph.FEATHER, PassiveGlyph.HEART);
	}

	@Override
	public @Nullable BleedProfile getMeleeBleed(ServerPlayer attacker) {
		return new BleedProfile(0.20f, 0);
	}

}
