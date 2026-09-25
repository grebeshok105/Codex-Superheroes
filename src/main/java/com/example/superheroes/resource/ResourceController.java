package com.example.superheroes.resource;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.transform.HeroData;
import com.example.superheroes.transform.HeroDataStore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResourceController {
	private ResourceController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	private static void tick(ServerPlayer player) {
		HeroData data = HeroDataStore.get(player);
		if (!data.hasHero()) {
			return;
		}
		ResourceLocation heroId = data.heroId();
		Hero hero = Heroes.get(heroId);
		if (hero == null) {
			return;
		}
		if (data.energy() < hero.getEnergyMax() && !EnergyLocks.isLocked(player)) {
			HeroDataStore.update(player, d -> d.withEnergy(
					Math.min(hero.getEnergyMax(), d.energy() + hero.getEnergyRegenPerTick())));
		}
		boolean madness = ModEffects.isMadness(player);
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
			float cost = madness ? 0f : ability.costPerTick();
			if (cost > 0f) {
				ResourceKind kind = current.binding(abilityId, hero.getDefaultBinding(abilityId));
				ResourcePayment payment = ResourcePayment.pay(current.energy(), current.mana(), kind, cost);
				if (!payment.success()) {
					AbilityRouter.deactivate(player, abilityId);
					continue;
				}
				HeroDataStore.update(player, d -> d.withResources(payment.energy(), payment.mana()));
			}
			ability.onTickActive(player);
		}
	}

	public static boolean tryConsume(ServerPlayer player, ResourceLocation abilityId, float amount) {
		return charge(player, abilityId, amount) != null;
	}

	/**
	 * Spends {@code amount} following the ability's binding.
	 *
	 * @return what was taken from each pool, or {@code null} when the player cannot pay
	 */
	@Nullable
	public static ResourcePayment charge(ServerPlayer player, ResourceLocation abilityId, float amount) {
		if (amount <= 0f || ModEffects.isMadness(player)) {
			return ResourcePayment.pay(0f, 0f, ResourceKind.ENERGY, 0f);
		}
		HeroData data = HeroDataStore.get(player);
		if (!data.hasHero()) {
			return null;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return null;
		}
		ResourceKind kind = data.binding(abilityId, hero.getDefaultBinding(abilityId));
		ResourcePayment payment = ResourcePayment.pay(data.energy(), data.mana(), kind, amount);
		if (!payment.success()) {
			return null;
		}
		HeroDataStore.update(player, d -> d.withResources(payment.energy(), payment.mana()));
		return payment;
	}

	/** Gives back exactly what {@link #charge} took, on top of whatever happened since. */
	public static void refund(ServerPlayer player, ResourcePayment payment) {
		if (payment.energySpent() <= 0f && payment.manaSpent() <= 0f) {
			return;
		}
		HeroDataStore.update(player, d -> d.withResources(d.energy() + payment.energySpent(),
				d.mana() + payment.manaSpent()));
	}
}
