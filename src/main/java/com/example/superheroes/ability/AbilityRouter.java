package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.resource.EnergyLocks;
import com.example.superheroes.resource.ResourceController;
import com.example.superheroes.resource.ResourceKind;
import com.example.superheroes.transform.HeroData;
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null || !hero.getAbilities().contains(abilityId)) {
			return;
		}
		if (hero instanceof com.example.superheroes.hero.DoomsdayHero dh
				&& !dh.isAbilityUnlocked(player, abilityId)) {
			return;
		}
		if (hero instanceof com.example.superheroes.hero.ThanosHero th
				&& !th.isAbilityUnlocked(player, abilityId)) {
			com.example.superheroes.hero.ThanosHero.notifyMissingStone(player, abilityId);
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
		if (data.isActive(AbilityIds.IRON_FISTS) && !abilityId.equals(AbilityIds.IRON_FISTS)) {
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
		if (cost > 0f) {
			if (!canPayActivationCost(player, data, hero, abilityId, binding, cost)) {
				return;
			}
			if (!ResourceController.tryConsume(player, abilityId, cost)) {
				return;
			}
		}
		boolean ok = ability.tryActivate(player);
		if (!ok) {
			if (cost > 0f) {
				restoreActivationCost(player, data.energy(), data.mana());
			}
			return;
		}
		if (ability.isToggle()) {
			HeroData updated = player.getAttachedOrCreate(ModAttachments.HERO_DATA).withActive(abilityId, true);
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncHeroData(player, updated);
		}
	}

	public static void deactivate(ServerPlayer player, ResourceLocation abilityId) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.isActive(abilityId)) {
			return;
		}
		Ability ability = AbilityRegistry.get(abilityId);
		if (ability == null) {
			return;
		}
		ability.onDeactivate(player);
		HeroData updated = data.withActive(abilityId, false);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncHeroData(player, updated);
	}

	public static void bind(ServerPlayer player, ResourceLocation abilityId, ResourceKind kind) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null || !hero.getAbilities().contains(abilityId)) {
			return;
		}
		HeroData updated = data.withBinding(abilityId, kind);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncHeroData(player, updated);
	}

	private static boolean canPayActivationCost(ServerPlayer player, HeroData data, Hero hero,
			ResourceLocation abilityId, ResourceKind binding, float cost) {
		if (cost <= 0f || ModEffects.isMadness(player)) {
			return true;
		}
		if (!abilityId.equals(AbilityIds.UNIBEAM) && hero.getAbilities().contains(AbilityIds.UNIBEAM)
				&& binding == ResourceKind.ENERGY && data.energy() < cost + 100f) {
			return false;
		}
		float energy = data.energy();
		float mana = data.mana();
		if (binding == ResourceKind.ENERGY) {
			if (energy >= cost) {
				return true;
			}
			return mana >= cost - energy;
		}
		if (mana >= cost) {
			return true;
		}
		return energy >= cost - mana;
	}

	private static void restoreActivationCost(ServerPlayer player, float energy, float mana) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		HeroData updated = data.withResources(energy, mana);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncResources(player, updated);
	}
}
