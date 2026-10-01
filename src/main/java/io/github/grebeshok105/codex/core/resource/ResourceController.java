package io.github.grebeshok105.codex.core.resource;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.model.ResourceKind;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ResourceController {
	private static final List<Predicate<ServerPlayer>> FREE_COST = new ArrayList<>();

	private ResourceController() {
	}

	/** States that make every ability free while active (madness). Registration order is evaluation order. */
	public static void freeCost(Predicate<ServerPlayer> rule) {
		FREE_COST.add(rule);
	}

	public static boolean isFree(ServerPlayer player) {
		for (Predicate<ServerPlayer> rule : FREE_COST) {
			if (rule.test(player)) {
				return true;
			}
		}
		return false;
	}

	/** Energy regen; the per-active-ability drain is {@code AbilityRouter.tickActive}. */
	public static void tick(ServerPlayer player) {
		HeroData data = HeroDataStore.get(player);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return;
		}
		if (data.energy() < hero.getEnergyMax() && !EnergyLocks.isLocked(player)) {
			HeroDataStore.update(player, d -> d.withEnergy(
					Math.min(hero.getEnergyMax(),
							d.energy() + hero.getEnergyRegenPerTick() + hero.energyRegenBonus(player))));
		}
	}

	public static boolean tryConsume(ServerPlayer player, ResourceLocation abilityId, float amount) {
		return charge(player, abilityId, amount) != null;
	}

	/**
	 * Spends {@code amount} following the ability's binding.
	 *
	 * @return what was taken from each pool, or {@code null} when the player cannot pay
	 *         or the energy pool is locked (mirrors the router's {@link EnergyLocks} gate —
	 *         authored-cast abilities charge on a later tick, past the router's check)
	 */
	@Nullable
	public static ResourcePayment charge(ServerPlayer player, ResourceLocation abilityId, float amount) {
		if (amount <= 0f || isFree(player)) {
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
		if (kind == ResourceKind.ENERGY && EnergyLocks.isLocked(player)) {
			return null;
		}
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
