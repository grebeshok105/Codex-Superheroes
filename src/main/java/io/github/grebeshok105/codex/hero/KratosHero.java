package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class KratosHero implements Hero {
	public static final ResourceLocation ID = ModId.of("kratos");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/kratos.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0200808,
			0xD00C0202,
			0x88AA1010,
			0x33FFB060,
			0xFFE03030,
			0xFF601010,
			0xFFE03030,
			0x55FF7060,
			0xFFE03030,
			0xFF1A0202,
			0xFFFFB060,
			0x55FFCC80,
			0xFFFFB060,
			0x55AA1010,
			0xFFFFB060,
			0xFFFFB060,
			0xFFFFFFFF,
			0x55FF7060
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.spartan_rage", HeroHudConfig.EnergyIconType.FLAME, true, "GOD SLAYER");

	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/kratos/armor");
	private static final ResourceLocation TOUGHNESS_ID = ModId.of("modifiers/kratos/toughness");
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/kratos/damage");
	private static final ResourceLocation ATTACK_SPEED_ID = ModId.of("modifiers/kratos/attack_speed");
	private static final ResourceLocation HP_ID = ModId.of("modifiers/kratos/max_health");
	private static final ResourceLocation KNOCKBACK_ID = ModId.of("modifiers/kratos/knockback_resistance");
	private static final ResourceLocation REACH_ID = ModId.of("modifiers/kratos/entity_reach");
	private static final ResourceLocation STEP_ID = ModId.of("modifiers/kratos/step_height");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ARMOR, ARMOR_ID, 18.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 6.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.MAX_HEALTH, HP_ID, 30.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, 1.5, AttributeModifier.Operation.ADD_VALUE)
			.add(Attributes.STEP_HEIGHT, STEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
			.build();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 250f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.6f;
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
			default -> EntityDimensions.scalable(0.65f, 1.95f).withEyeHeight(1.75f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.KRATOS_SPARTAN_RAGE,
				AbilityIds.KRATOS_BLADE_STORM,
				AbilityIds.KRATOS_CHAIN_WHIRL,
				AbilityIds.KRATOS_LEVIATHAN_THROW,
				AbilityIds.KRATOS_GOD_SLAYER
		);
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
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		PASSIVES.remove(player);
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
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public double getImpactPower() {
		return 1.16;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.SWORD, PassiveGlyph.SHIELD, PassiveGlyph.BOLT);
	}

	@Override
	public boolean canSuperJump() {
		return true;
	}

	@Override
	public @Nullable BleedProfile getMeleeBleed(ServerPlayer attacker) {
		return new BleedProfile(0.25f, 0);
	}

}
