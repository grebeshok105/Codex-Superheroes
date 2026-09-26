package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class NarutoHero implements Hero {
	public static final ResourceLocation ID = ModId.of("naruto");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/naruto.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0332B00,
			0xD0141000,
			0x88FFD60A,
			0x33FFE99A,
			0xFFFFE85A,
			0xFF7A6700,
			0xFFFFD60A,
			0x55FFEC8A,
			0xFFFFE060,
			0xFF1A1500,
			0xFFFFCC1A,
			0x55FFE070,
			0xFFFFCC1A,
			0x55FFD60A,
			0xFFFFE060,
			0xFFFFE060,
			0xFFFFF7C0,
			0x55FFE070
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.chakra", HeroHudConfig.EnergyIconType.SPIRAL, true, "BIJUUDAMA");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 200f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 1.8f;
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
			default -> EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.NARUTO_RASENGAN,
				AbilityIds.NARUTO_OODAMA_RASENGAN,
				AbilityIds.NARUTO_RASENSHURIKEN,
				AbilityIds.NARUTO_SAGE_MODE,
				AbilityIds.NARUTO_BIJUUDAMA,
				AbilityIds.NARUTO_SHADOW_CLONES
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.NARUTO.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.NARUTO.remove(player);
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
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.B;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.BOLT, PassiveGlyph.SKULL);
	}

	@Override
	public boolean canSuperJump() {
		return true;
	}

}
