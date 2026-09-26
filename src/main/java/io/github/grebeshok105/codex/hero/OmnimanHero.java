package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.ability.OmnimanViltrumiteRushAbility;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.BleedProfile;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroHudConfig;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.core.hero.LandingImpact;
import io.github.grebeshok105.codex.core.hero.PassiveGlyph;
import io.github.grebeshok105.codex.effect.OmnimanMomentumController;
import io.github.grebeshok105.codex.physics.ShockwaveUtil;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
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

public final class OmnimanHero implements Hero {
	public static final ResourceLocation ID = ModId.of("omniman");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/omniman.png");
	public static final HeroTheme THEME = new HeroTheme(
			0xE00F1720,
			0xD0081018,
			0xAA7A0F18,
			0x55E5D0C8,
			0xFFE5D0C8,
			0xFF2A333D,
			0xFFB01C28,
			0x66D93642,
			0xFFFFE6DA,
			0xFF161C22,
			0xFFC9D1D9,
			0x66C9D1D9,
			0xFFE6D5CB,
			0x66840E18,
			0xFFC9D1D9,
			0xFFE6D5CB,
			0xFFFFFFFF,
			0x66B01C28
	);

	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.viltrumite_power", HeroHudConfig.EnergyIconType.FIST, true, "WORLD BREAKER");

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/omniman/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/omniman/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/omniman/damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/omniman/speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/omniman/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/omniman/knockback_resistance");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/omniman/attack_speed");
	private static final ResourceLocation JUMP_ID = ModId.of("modifiers/omniman/jump_strength");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/omniman/step_height");
	private static final ResourceLocation REACH_ID = ModId.of("modifiers/omniman/entity_reach");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 16.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MOVEMENT_SPEED, SPEED_ID, 0.32, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.add(Attributes.MAX_HEALTH, HP_ID, 60.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 0.8, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.JUMP_STRENGTH, JUMP_ID, 0.35, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 0.6, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, 0.8, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 320f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.5f;
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
			default -> EntityDimensions.scalable(0.6f, 1.88f).withEyeHeight(1.68f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.FLIGHT,
				AbilityIds.OMNIMAN_VILTRUMITE_RUSH,
				AbilityIds.OMNIMAN_THINK_MARK,
				AbilityIds.OMNIMAN_WORLD_BREAKER,
				AbilityIds.VILTRUMITE_RECOVERY);
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
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
		player.removeEffect(MobEffects.REGENERATION);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
		if (player instanceof ServerPlayer sp) {
			OmnimanMomentumController.clear(sp);
			OmnimanViltrumiteRushAbility.clear(sp);
			io.github.grebeshok105.codex.ability.OmnimanThinkMarkAbility.clear(sp);
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
		float momentum = OmnimanMomentumController.momentum(player) / 100f;
		float scale = 0.45f + intensity * 1.45f + momentum * 0.45f;
		double radius = 4.0 + scale * 9.0;
		float damage = 7.0f + scale * 13.0f;
		ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();
		level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.55f);
		level.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 0.85f);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 0.3, cz, 2, radius * 0.35, 0.15, radius * 0.35, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.15, cz, 70, radius * 0.55, 0.25, radius * 0.55, 0.1);
		level.sendParticles(ParticleTypes.CRIT, cx, cy + 0.65, cz, 34, radius * 0.35, 0.2, radius * 0.35, 0.16);
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
		return 1.27;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.S;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.FEATHER, PassiveGlyph.HEART);
	}

	@Override
	public @Nullable BleedProfile getMeleeBleed(ServerPlayer attacker) {
		return new BleedProfile(0.40f, 1);
	}

}
