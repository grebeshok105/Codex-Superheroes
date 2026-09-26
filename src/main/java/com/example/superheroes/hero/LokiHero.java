package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class LokiHero implements Hero {
	public static final ResourceLocation ID = ModId.of("loki");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/loki.png");

	private static final HeroTheme THEME = new HeroTheme(
			0xE0102008,
			0xD0040A02,
			0x881E8030,
			0x3360E060,
			0xFF60E060,
			0xFF1E5020,
			0xFF40C040,
			0x5560E060,
			0xFF60E060,
			0xFF200818,
			0xFFFFD030,
			0x55FFE070,
			0xFFFFD030,
			0x551E8030,
			0xFFFFD030,
			0xFFFFD030,
			0xFFFFFFFF,
			0x5560E060
	);
	private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.magic", HeroHudConfig.EnergyIconType.MAGIC, true, "CHAOS BOLT");

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
		return 2.0f;
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
				AbilityIds.LOKI_ASTRAL_CLONES,
				AbilityIds.LOKI_TESSERACT_BLINK,
				AbilityIds.LOKI_MIND_CHARM,
				AbilityIds.LOKI_GLAMOUR,
				AbilityIds.LOKI_CHAOS_BOLT
		);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.LOKI.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.LOKI.remove(player);
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
		return 0.95;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.D;
	}

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.MAGIC, PassiveGlyph.FEATHER, PassiveGlyph.BOLT);
	}

}
