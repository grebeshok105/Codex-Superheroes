package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Pandora (Re:Zero). The Mirror Dimension feature
 * becomes the "House of Vanity" (Дом тщеславия): inside it Pandora wields her
 * Authority of Greed. The body is rendered at 75% of a vanilla Steve via the
 * vanilla {@code minecraft:generic.scale} attribute (the {@code PASSIVES} set below),
 * which scales both the visual model and the hitbox/eye-height in one shot.
 *
 * <p>Skin is a classic/4px (WIDE) player skin override — the client skin mixin
 * already forces {@code PlayerSkin.Model.WIDE} for hero textures.
 */
public final class PandoraHero implements Hero {
	public static final ResourceLocation ID = ModId.of("pandora");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/pandora.png");

	// Same palette as Regulus on purpose (House of Vanity shares his madness colors).
	private static final HeroTheme THEME = new HeroTheme(
			0xFFFFFFFF,
			0xFF606060,
			0xFFFFFFFF,
			0x44FFFFFF,
			0xFFFFFFFF,
			0xFFCCCCCC,
			0xFFFFFFFF,
			0x66FFFFFF,
			0xFFFFFFFF,
			0xFF555555,
			0xFFFFFFFF,
			0x66E0E0E0,
			0xFF555555,
			0x66E0E0E0,
			0xFFFFFFFF,
			0xFFFFFFFF,
			0xFFFFFFFF,
			0x66FFFFFF
	);

	// Pandora is a "naked Steve": vanilla 20 HP, no armor / toughness / damage
	// bonuses. Her power comes entirely from the House of Vanity, not raw stats.
	// (Only the 75% body scale is kept — purely visual/hitbox, not a combat buff.)
	private static final ResourceLocation SCALE_ID = ModId.of("modifiers/pandora/body_scale");

	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.SCALE, SCALE_ID, -0.25, AttributeModifier.Operation.ADD_VALUE)
			.build();

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
	public AttributeModifierSet passiveAttributes() {
		return PASSIVES;
	}

	@Override
	public void applyPassives(Player player) {
		PASSIVES.apply(player);
		if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
			io.github.grebeshok105.codex.effect.PandoraDeathController.reapplyState(sp);
		}
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return false;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}

	@Override
	public HeroTheme getTheme() {
		// Pandora's HUD/radial colours are pure white — same monochrome theme as Regulus.
		return THEME;
	}
	@Override
	public boolean canUseAbility(net.minecraft.server.level.ServerPlayer player,
			io.github.grebeshok105.codex.core.transform.HeroData data, ResourceLocation abilityId) {
		return !isDimensionOnly(abilityId)
				|| io.github.grebeshok105.codex.effect.MirrorDimensionController.hasActiveHouse(player);
	}

	@Override
	public AbilityAvailability.Visibility visibility(net.minecraft.server.level.ServerPlayer player,
			ResourceLocation abilityId) {
		return (!isDimensionOnly(abilityId)
				|| io.github.grebeshok105.codex.effect.MirrorDimensionController.hasActiveHouse(player))
				? AbilityAvailability.Visibility.AVAILABLE
				: AbilityAvailability.Visibility.HIDDEN;
	}

	@Override
	public void onAbilityDenied(net.minecraft.server.level.ServerPlayer player, ResourceLocation abilityId) {
		player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
				"ability.superheroes.pandora.not_in_house").withStyle(net.minecraft.ChatFormatting.DARK_GRAY), true);
	}

}
