package io.github.grebeshok105.codex.core.ability;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.core.model.ResourceKind;
import io.github.grebeshok105.codex.core.resource.ResourcePayment;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AbilityRouter {
	private AbilityRouter() {
	}

	public static void activate(ServerPlayer player, ResourceLocation abilityId) {
		AbilityDenial blocked = AbilityRules.firstBlock(player, abilityId);
		if (blocked != null) {
			blocked.notify(player);
			return;
		}
		HeroData data = HeroDataStore.get(player);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null || !hero.getAbilities().contains(abilityId)) {
			return;
		}
		// Hero-specific gates live on the hero (audit debt 4): Doomsday tiers, stone
		// stones, Pandora's dimension-only powers.
		if (!hero.canUseAbility(player, data, abilityId)) {
			hero.onAbilityDenied(player, abilityId);
			return;
		}
		Ability ability = AbilityRegistry.get(abilityId);
		if (ability == null) {
			return;
		}
		if (ability.isToggle() && data.isActive(abilityId)) {
			deactivate(player, abilityId);
			return;
		}
		if (hero.isAbilitySuppressedBy(data, abilityId)) {
			return;
		}
		if (AbilityCooldowns.isOnCooldown(player, abilityId)) {
			return;
		}
		ResourceKind binding = data.binding(abilityId, hero.getDefaultBinding(abilityId));
		if (EnergyLocks.isLocked(player) && binding == ResourceKind.ENERGY
				&& (ability.costOnActivate() > 0f || ability.costPerTick() > 0f)) {
			return;
		}
		if (!ability.canActivate(player)) {
			return;
		}
		float cost = ability.costOnActivate();
		ResourcePayment payment = null;
		if (cost > 0f) {
			if (!canPayActivationCost(player, data, hero, abilityId, binding, cost)) {
				return;
			}
			payment = ResourceController.charge(player, abilityId, cost);
			if (payment == null) {
				return;
			}
		}
		boolean ok = ability.tryActivate(player);
		if (!ok) {
			if (payment != null) {
				// Refund the delta, not a pre-activation snapshot: tryActivate may have changed resources itself.
				ResourceController.refund(player, payment);
			}
			return;
		}
		if (ability.isToggle()) {
			HeroDataStore.update(player, d -> d.withActive(abilityId, true));
		}
	}

	/**
	 * Marks the ability inactive, then runs its {@code onDeactivate}. Clearing first makes nested
	 * deactivation a no-op and lets {@code onDeactivate} deliberately re-assert the ability
	 * (Rem's permanent demonism) without being overwritten afterwards.
	 */
	public static void deactivate(ServerPlayer player, ResourceLocation abilityId) {
		if (!HeroDataStore.get(player).isActive(abilityId)) {
			return;
		}
		HeroDataStore.update(player, d -> d.withActive(abilityId, false));
		Ability ability = AbilityRegistry.get(abilityId);
		if (ability != null) {
			ability.onDeactivate(player);
		}
	}

	/**
	 * Per-player ability tick: charges every active toggle's cost-per-tick and runs its
	 * {@code onTickActive}. Registered after {@code ResourceController.tick} so regen lands
	 * before the drain.
	 */
	public static void tickActive(ServerPlayer player) {
		HeroData data = HeroDataStore.get(player);
		if (!data.hasHero()) {
			return;
		}
		ResourceLocation heroId = data.heroId();
		Hero hero = Heroes.get(heroId);
		if (hero == null) {
			return;
		}
		boolean free = ResourceController.isFree(player);
		List<ResourceLocation> active = new ArrayList<>(data.activeAbilities());
		active.sort(Comparator.comparing(ResourceLocation::toString));
		for (ResourceLocation abilityId : active) {
			// Re-read every time: an earlier callback may have deactivated abilities, spent resources or
			// even untransformed the player. Writing back the tick-start copy is what resurrected B2.
			HeroData current = HeroDataStore.get(player);
			if (!heroId.equals(current.heroId())) {
				return;
			}
			if (!current.isActive(abilityId)) {
				continue;
			}
			Ability ability = AbilityRegistry.get(abilityId);
			if (ability == null) {
				continue;
			}
			float cost = free ? 0f : ability.costPerTick();
			if (cost > 0f) {
				ResourceKind kind = current.binding(abilityId, hero.getDefaultBinding(abilityId));
				ResourcePayment payment = ResourcePayment.pay(current.energy(), current.mana(), kind, cost);
				if (!payment.success()) {
					deactivate(player, abilityId);
					continue;
				}
				HeroDataStore.update(player, d -> d.withResources(payment.energy(), payment.mana()));
			}
			ability.onTickActive(player);
		}
	}

	/**
	 * Runs {@code onDeactivate} on every active ability without touching the active set —
	 * transform fires it through {@code HeroTransformService}'s deactivator seam.
	 */
	public static void deactivateAll(ServerPlayer player, HeroData data) {
		for (ResourceLocation activeId : data.activeAbilities()) {
			Ability ability = AbilityRegistry.get(activeId);
			if (ability != null) {
				ability.onDeactivate(player);
			}
		}
	}

	public static void bind(ServerPlayer player, ResourceLocation abilityId, ResourceKind kind) {
		HeroData data = HeroDataStore.get(player);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null || !hero.getAbilities().contains(abilityId)) {
			return;
		}
		HeroDataStore.update(player, d -> d.withBinding(abilityId, kind));
	}

	private static boolean canPayActivationCost(ServerPlayer player, HeroData data, Hero hero,
			ResourceLocation abilityId, ResourceKind binding, float cost) {
		if (cost <= 0f || ResourceController.isFree(player)) {
			return true;
		}
		if (binding == ResourceKind.ENERGY
				&& data.energy() < cost + hero.getEnergyReserveFor(abilityId, binding)) {
			return false;
		}
		return ResourcePayment.pay(data.energy(), data.mana(), binding, cost).success();
	}
}
