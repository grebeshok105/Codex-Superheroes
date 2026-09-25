package com.example.superheroes.transform;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class HeroTransformService {
	private static final int COOLDOWN_TICKS = 20;

	private HeroTransformService() {
	}

	public static boolean transform(ServerPlayer player, ResourceLocation heroId) {
		Hero hero = Heroes.get(heroId);
		if (hero == null) {
			return false;
		}
		if (isOnCooldown(player)) {
			return false;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero() && heroId.equals(data.heroId())) {
			return false;
		}
		if (data.hasHero()) {
			Hero current = Heroes.get(data.heroId());
			if (current != null) {
				current.removePassives(player);
				deactivateAll(player, data);
			}
		}
		Map<ResourceLocation, ResourceKind> bindings = new HashMap<>(data.abilityBindings());
		for (ResourceLocation abilityId : hero.getAbilities()) {
			bindings.putIfAbsent(abilityId, hero.getDefaultBinding(abilityId));
		}
		HeroData updated = HeroDataStore.update(player, d -> new HeroData(
				Optional.of(heroId),
				// carried over like mana on a hero swap — a swap must not be a free refill
				// (audit B5); a fresh transform still starts with full energy
				d.hasHero() ? Math.min(d.energy(), hero.getEnergyMax()) : hero.getEnergyMax(),
				Math.min(d.mana(), hero.getManaMax()),
				bindings,
				java.util.Set.of()
		));
		clearHeroRuntimeState(player);
		com.example.superheroes.lifecycle.PassiveReconciler.applyAndCapture(player, hero);
		player.refreshDimensions();
		// keep absolute health — transforming must not be a free heal (audit B5)
		player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
		playTransformFx(player, true);
		com.example.superheroes.lifecycle.HeroLifecycle.fireTransformed(player, heroId);
		markTransformed(player);
		return true;
	}

	public static boolean untransform(ServerPlayer player) {
		if (isOnCooldown(player)) {
			return false;
		}
		return doUntransform(player, true);
	}

	public static boolean forceUntransform(ServerPlayer player) {
		return doUntransform(player, false);
	}

	private static boolean doUntransform(ServerPlayer player, boolean playFx) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return false;
		}
		Hero current = Heroes.get(data.heroId());
		if (current != null) {
			current.removePassives(player);
			deactivateAll(player, data);
		}
		clearHeroRuntimeState(player);
		com.example.superheroes.lifecycle.PassiveReconciler.clear(player.getUUID());
		HeroDataStore.update(player, d -> d.withHero(null).withResources(0f, 0f).clearActive());
		player.refreshDimensions();
		if (playFx) {
			playTransformFx(player, false);
		}
		markTransformed(player);
		return true;
	}

	private static boolean isOnCooldown(ServerPlayer player) {
		Long last = player.getAttached(ModAttachments.TRANSFORM_TICK);
		if (last == null) {
			return false;
		}
		return player.server.getTickCount() - last < COOLDOWN_TICKS;
	}

	private static void markTransformed(ServerPlayer player) {
		player.setAttached(ModAttachments.TRANSFORM_TICK, (long) player.server.getTickCount());
	}

	private static void playTransformFx(ServerPlayer player, boolean activate) {
		ServerLevel level = player.serverLevel();
		if (activate) {
			level.sendParticles(ModParticles.TRANSFORM_SPARK,
					player.getX(), player.getY() + 0.5, player.getZ(),
					60, 0.8, 0.8, 0.8, 0.15);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1f);
		} else {
			level.sendParticles(ParticleTypes.SMOKE,
					player.getX(), player.getY() + 0.5, player.getZ(),
					30, 0.5, 0.5, 0.5, 0.05);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1f);
		}
	}

	public static void onPlayerJoin(ServerPlayer player) {
		HeroData data = HeroDataStore.get(player);
		if (data.hasHero() && !data.activeAbilities().isEmpty()) {
			data = HeroDataStore.update(player, HeroData::clearActive);
		}
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null) {
				reapplyLifecyclePassives(player, hero);
			}
		}
		HeroDataStore.syncFull(player);
	}

	public static void onPlayerRespawn(ServerPlayer newPlayer) {
		HeroData data = HeroDataStore.get(newPlayer);
		if (data.hasHero() && !data.activeAbilities().isEmpty()) {
			data = HeroDataStore.update(newPlayer, HeroData::clearActive);
		}
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null) {
				reapplyLifecyclePassives(newPlayer, hero);
			}
		}
		com.example.superheroes.effect.ReinhardController.onRespawn(newPlayer);
		HeroDataStore.syncFull(newPlayer);
	}

	private static void reapplyLifecyclePassives(ServerPlayer player, Hero hero) {
		if (DoomsdayHero.ID.equals(hero.getId())) {
			com.example.superheroes.lifecycle.PassiveReconciler.applyAndCapture(player, hero);
			return;
		}
		hero.removePassives(player);
		com.example.superheroes.lifecycle.PassiveReconciler.applyAndCapture(player, hero);
	}

	/**
	 * Server-thread leave hook ({@link com.example.superheroes.lifecycle.PlayerLifecycle#onLeave}).
	 * Game-state cleanup must not run on {@code ServerPlayConnectionEvents.DISCONNECT} — Fabric
	 * can fire that on the Netty thread. Clears session-scoped state without running ability
	 * {@code onDeactivate} (its gameplay side-effects would persist onto a leaving player).
	 */
	public static void onPlayerLeave(ServerPlayer player) {
		java.util.UUID id = player.getUUID();
		// ability cooldowns intentionally persist — they live on the player attachment (audit B5)
		com.example.superheroes.resource.EnergyLocks.clear(id);
		com.example.superheroes.effect.RemDemonismController.clear(player);
		com.example.superheroes.effect.UnibeamController.clearState(id);
		if (HeroDataStore.get(player).hasHero()) {
			HeroDataStore.update(player, HeroData::clearActive);
		}
	}

	/**
	 * Drop every hero-scoped session-state bit for the player (swap/untransform).
	 * Subscribers register via {@link com.example.superheroes.lifecycle.HeroLifecycle#onClear}
	 * (audit B23: transform used to clear a subset of what untransform cleared);
	 * ability cooldowns deliberately survive — persistent deadlines, not session state.
	 */
	public static void clearHeroRuntimeState(ServerPlayer player) {
		com.example.superheroes.lifecycle.HeroLifecycle.fireClear(player);
	}

	private static void deactivateAll(ServerPlayer player, HeroData data) {
		for (ResourceLocation activeId : data.activeAbilities()) {
			Ability ability = AbilityRegistry.get(activeId);
			if (ability != null) {
				ability.onDeactivate(player);
			}
		}
	}
}
