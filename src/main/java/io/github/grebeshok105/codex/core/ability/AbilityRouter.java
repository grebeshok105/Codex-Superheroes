package io.github.grebeshok105.codex.core.ability;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
import io.github.grebeshok105.codex.core.resource.ResourcePayment;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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
		// Hero-specific gates live on the hero (audit debt 4): Doomsday tiers, Thanos
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
		if (cost <= 0f || AbilityRules.isFree(player)) {
			return true;
		}
		if (binding == ResourceKind.ENERGY
				&& data.energy() < cost + hero.getEnergyReserveFor(abilityId, binding)) {
			return false;
		}
		return ResourcePayment.pay(data.energy(), data.mana(), binding, cost).success();
	}
}
