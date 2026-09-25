package com.example.superheroes.ability;

import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.resource.EnergyLocks;
import com.example.superheroes.resource.ResourceController;
import com.example.superheroes.resource.ResourceKind;
import com.example.superheroes.resource.ResourcePayment;
import com.example.superheroes.transform.HeroData;
import com.example.superheroes.transform.HeroDataStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AbilityRouter {
	private AbilityRouter() {
	}

	public static void activate(ServerPlayer player, ResourceLocation abilityId) {
		if (ModEffects.isAftermath(player)) {
			return;
		}
		if (player.hasEffect(ModEffects.DISABLED_ABILITIES)) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
					"ability.superheroes.disabled_by_snap").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
			return;
		}
		if (player.hasEffect(ModEffects.VANITY_STRIPPED)) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
					"ability.superheroes.vanity_stripped").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
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
		if (cost <= 0f || ModEffects.isMadness(player)) {
			return true;
		}
		if (binding == ResourceKind.ENERGY
				&& data.energy() < cost + hero.getEnergyReserveFor(abilityId, binding)) {
			return false;
		}
		return ResourcePayment.pay(data.energy(), data.mana(), binding, cost).success();
	}
}
