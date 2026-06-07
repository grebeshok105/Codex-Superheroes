package com.example.superheroes.resource;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return;
		}
		float energy = data.energy();
		float mana = data.mana();
		boolean dirty = false;
		if (energy < hero.getEnergyMax() && !EnergyLocks.isLocked(player)) {
			energy = Math.min(hero.getEnergyMax(), energy + hero.getEnergyRegenPerTick());
			dirty = true;
		}
		Set<ResourceLocation> active = new HashSet<>(data.activeAbilities());
		Set<ResourceLocation> toDeactivate = new HashSet<>();
		boolean madness = ModEffects.isMadness(player);
		for (ResourceLocation abilityId : active) {
			Ability ability = AbilityRegistry.get(abilityId);
			if (ability == null) {
				continue;
			}
			float cost = madness ? 0f : ability.costPerTick();
			if (cost > 0f) {
				ResourceKind kind = data.binding(abilityId, hero.getDefaultBinding(abilityId));
				ConsumeResult cr = consume(energy, mana, kind, cost);
				if (!cr.success()) {
					ability.onDeactivate(player);
					toDeactivate.add(abilityId);
					dirty = true;
					continue;
				}
				energy = cr.energy();
				mana = cr.mana();
				dirty = true;
			}
			ability.onTickActive(player);
		}
		if (dirty) {
			HeroData updated = data.withResources(energy, mana);
			for (ResourceLocation abilityId : toDeactivate) {
				updated = updated.withActive(abilityId, false);
			}
			player.setAttached(ModAttachments.HERO_DATA, updated);
			if (!toDeactivate.isEmpty()) {
				ModNetworking.syncHeroData(player, updated);
			} else {
				ModNetworking.syncResources(player, updated);
			}
		}
	}

	public static boolean tryConsume(ServerPlayer player, ResourceLocation abilityId, float amount) {
		if (amount <= 0f) {
			return true;
		}
		if (ModEffects.isMadness(player)) {
			return true;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return false;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return false;
		}
		ResourceKind kind = data.binding(abilityId, hero.getDefaultBinding(abilityId));
		ConsumeResult cr = consume(data.energy(), data.mana(), kind, amount);
		if (!cr.success()) {
			return false;
		}
		HeroData updated = data.withResources(cr.energy(), cr.mana());
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncResources(player, updated);
		return true;
	}

	private static ConsumeResult consume(float energy, float mana, ResourceKind preferred, float amount) {
		if (preferred == ResourceKind.ENERGY) {
			if (energy >= amount) {
				return new ConsumeResult(true, energy - amount, mana);
			}
			float deficit = amount - energy;
			if (mana >= deficit) {
				return new ConsumeResult(true, 0f, mana - deficit);
			}
		} else {
			if (mana >= amount) {
				return new ConsumeResult(true, energy, mana - amount);
			}
			float deficit = amount - mana;
			if (energy >= deficit) {
				return new ConsumeResult(true, energy - deficit, 0f);
			}
		}
		return new ConsumeResult(false, energy, mana);
	}

	private record ConsumeResult(boolean success, float energy, float mana) {
	}
}
