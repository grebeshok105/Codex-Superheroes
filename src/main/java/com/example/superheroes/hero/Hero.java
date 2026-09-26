package com.example.superheroes.hero;

import com.example.superheroes.core.ability.AbilityAvailability;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Hero {
	ResourceLocation getId();

	float getEnergyMax();

	float getEnergyRegenPerTick();

	float getManaMax();

	@Nullable
	EntityDimensions getDimensions(Pose pose);

	List<ResourceLocation> getAbilities();

	ResourceKind getDefaultBinding(ResourceLocation abilityId);

	/**
	 * Permanent attribute modifiers owned by this hero — what {@link #applyPassives}
	 * and {@link #removePassives} route through by default. Heroes whose passives
	 * carry extra logic (effects, tier-scaled values, other modifier sets) keep
	 * custom bodies that apply this set inside them. Empty when the hero has no
	 * attribute passives.
	 */
	default AttributeModifierSet passiveAttributes() {
		return AttributeModifierSet.builder().build();
	}

	default void applyPassives(Player player) {
		passiveAttributes().apply(player);
	}

	default void removePassives(Player player) {
		passiveAttributes().remove(player);
	}

	/**
	 * Whether a dead player keeps this hero: the global AFTER_DEATH untransform
	 * skips heroes whose own death pipeline adapts through death (Doomsday).
	 */
	default boolean keepsHeroOnDeath() {
		return false;
	}

	/**
	 * Re-applies passives on a fresh player entity (relog, respawn); callers wrap
	 * this in {@code PassiveReconciler.capture} so the declared infinite effects
	 * stay protected. Default re-derives from scratch: {@link #removePassives}
	 * then {@link #applyPassives}. A hero that keeps its transformation on death
	 * overrides this to apply-only — removing would reset the progress the death
	 * just granted (Doomsday's tier reset lives inside {@code removePassives}).
	 */
	default void reapplyPassivesAfterRespawn(ServerPlayer player) {
		removePassives(player);
		applyPassives(player);
	}

	boolean cancelsFallDamage(Player player);

	@Nullable
	default ResourceLocation getSkinTexture() {
		return null;
	}

	default void onLanded(ServerPlayer player, LandingImpact impact) {
	}

	default HeroTheme getTheme() {
		return HeroTheme.DEFAULT;
	}

	default HeroHudConfig getHudConfig() {
		return HeroHudConfig.DEFAULT;
	}

	/**
	 * Melee impact presentation for {@code CombatImpactEngine} — default hero-neutral
	 * {@link ImpactStyle#DEFAULT} at power 1.0.
	 */
	default com.example.superheroes.physics.ImpactStyle getImpactStyle() {
		return com.example.superheroes.physics.ImpactStyle.DEFAULT;
	}

	default double getImpactPower() {
		return 1.0;
	}

	/** J.A.R.V.I.S. threat classification shown by Iron Man's scan. */
	default JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.C;
	}

	/** Glyphs of this hero's passives in the info panel, in lang order {@code hero.<ns>.<id>.passive.<n>}. */
	default java.util.List<PassiveGlyph> getPassiveGlyphs() {
		return java.util.List.of();
	}

	/** Whether the super-jump key works for this hero. */
	default boolean canSuperJump() {
		return false;
	}

	/** Bleeding this hero's melee hit applies right now; {@code null} for none. */
	@Nullable
	default BleedProfile getMeleeBleed(ServerPlayer attacker) {
		return null;
	}

	/**
	 * Gate for {@code AbilityRouter.activate} — hero-specific locks (Doomsday tiers,
	 * Thanos stones, Pandora's dimension-only powers) live here, not in the router.
	 * On {@code false}, {@link #onAbilityDenied} runs for player feedback.
	 */
	default boolean canUseAbility(ServerPlayer player, com.example.superheroes.transform.HeroData data,
			ResourceLocation abilityId) {
		return true;
	}

	/** Feedback when {@link #canUseAbility} denied activation (message/sound). */
	default void onAbilityDenied(ServerPlayer player, ResourceLocation abilityId) {
	}

	/**
	 * Server-side visibility of {@code abilityId} on this player's HUD, synced to the owner
	 * through the {@code ability_availability} attachment (stage C4 — the hook that replaced
	 * the deleted client-side filter). Default {@code AVAILABLE}; heroes hiding abilities
	 * behind progression (Doomsday tiers, Thanos stones, Pandora's house, Rem's demonism,
	 * Regulus's madness) return {@code HIDDEN}/{@code LOCKED} instead.
	 */
	default AbilityAvailability.Visibility visibility(ServerPlayer player, ResourceLocation abilityId) {
		return AbilityAvailability.Visibility.AVAILABLE;
	}

	/**
	 * While-stance suppression: deny an ability because another one is active
	 * (Homelander's Iron Fists locks out everything else).
	 */
	default boolean isAbilitySuppressedBy(com.example.superheroes.transform.HeroData data,
			ResourceLocation abilityId) {
		return false;
	}

	/**
	 * Energy kept in reserve for a signature ability — activation of {@code abilityId}
	 * under {@code binding} fails when energy would drop below the reserve
	 * (Iron Man reserves 100 for Unibeam).
	 */
	default float getEnergyReserveFor(ResourceLocation abilityId,
			com.example.superheroes.resource.ResourceKind binding) {
		return 0f;
	}

	/** Uranium pressure forcibly cuts this hero's flight after a grace period. */
	default boolean isUraniumWeak() {
		return false;
	}
}
