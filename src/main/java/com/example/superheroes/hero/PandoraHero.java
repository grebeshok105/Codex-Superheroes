package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Pandora (Re:Zero) — replaces Doctor Strange. The Mirror Dimension feature
 * becomes the "House of Vanity" (Дом тщеславия): inside it Pandora wields her
 * Authority of Greed. The body is rendered at 75% of a vanilla Steve via the
 * vanilla {@code minecraft:generic.scale} attribute (see {@link HeroAttributes#PANDORA}),
 * which scales both the visual model and the hitbox/eye-height in one shot.
 *
 * <p>Skin is a classic/4px (WIDE) player skin override — the client skin mixin
 * already forces {@code PlayerSkin.Model.WIDE} for hero textures.
 */
public final class PandoraHero implements Hero {
	public static final ResourceLocation ID = ModId.of("pandora");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/pandora.png");

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
		return 1.0f;
	}

	@Override
	public float getManaMax() {
		return 0f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		// Base (vanilla-equivalent) dimensions; the 0.75 shrink is applied on top
		// by the SCALE attribute in applyPassives, so we must NOT pre-shrink here.
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
			default -> EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f);
		};
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		// "Дом тщеславия" (House of Vanity) + "Каприз творца" (Creator's Whim) +
		// dimension-only "Пространственная привязка" (rope bind) and
		// "Сжатие пространства" (space crush). The dimension-only ones are gated
		// in AbilityRouter so they only fire while the House is open.
		return List.of(
				AbilityIds.MIRROR_DIMENSION,
				AbilityIds.MIRROR_MODE_CYCLE,
				AbilityIds.SPATIAL_BIND,
				AbilityIds.SPACE_CRUSH,
				AbilityIds.VANITY_STRIP);
	}

	/**
	 * @return true if {@code abilityId} is one of Pandora's dimension-only powers,
	 *         which may only be used while her House of Vanity is open.
	 */
	public static boolean isDimensionOnly(ResourceLocation abilityId) {
		return AbilityIds.MIRROR_MODE_CYCLE.equals(abilityId)
				|| AbilityIds.SPATIAL_BIND.equals(abilityId)
				|| AbilityIds.SPACE_CRUSH.equals(abilityId)
				|| AbilityIds.VANITY_STRIP.equals(abilityId);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.PANDORA.apply(player);
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.PANDORA.remove(player);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return false;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}
}
