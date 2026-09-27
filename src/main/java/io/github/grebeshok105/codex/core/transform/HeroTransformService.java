package io.github.grebeshok105.codex.core.transform;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.lifecycle.HeroLifecycle;
import io.github.grebeshok105.codex.core.lifecycle.PassiveReconciler;
import io.github.grebeshok105.codex.core.lifecycle.PlayerLifecycle;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.particle.ModParticles;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
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
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
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
		io.github.grebeshok105.codex.core.lifecycle.PassiveReconciler.applyAndCapture(player, hero);
		player.refreshDimensions();
		// keep absolute health — transforming must not be a free heal (audit B5)
		player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
		playTransformFx(player, true);
		io.github.grebeshok105.codex.core.lifecycle.HeroLifecycle.fireTransformed(player, heroId);
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
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return false;
		}
		Hero current = Heroes.get(data.heroId());
		if (current != null) {
			current.removePassives(player);
			deactivateAll(player, data);
		}
		clearHeroRuntimeState(player);
		io.github.grebeshok105.codex.core.lifecycle.PassiveReconciler.clear(player.getUUID());
		HeroDataStore.update(player, d -> d.withHero(null).withResources(0f, 0f).clearActive());
		player.refreshDimensions();
		if (playFx) {
			playTransformFx(player, false);
		}
		markTransformed(player);
		return true;
	}

	private static boolean isOnCooldown(ServerPlayer player) {
		Long last = player.getAttached(CoreAttachments.TRANSFORM_TICK);
		if (last == null) {
			return false;
		}
		return player.server.getTickCount() - last < COOLDOWN_TICKS;
	}

	private static void markTransformed(ServerPlayer player) {
		player.setAttached(CoreAttachments.TRANSFORM_TICK, (long) player.server.getTickCount());
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
		HeroDataStore.syncFull(newPlayer);
	}

	private static void reapplyLifecyclePassives(ServerPlayer player, Hero hero) {
		io.github.grebeshok105.codex.core.lifecycle.PassiveReconciler.capture(player, hero.getId(),
				() -> hero.reapplyPassivesAfterRespawn(player));
	}

	/**
	 * Server-thread leave hook ({@link io.github.grebeshok105.codex.core.lifecycle.PlayerLifecycle#onLeave}).
	 * Game-state cleanup must not run on {@code ServerPlayConnectionEvents.DISCONNECT} — Fabric
	 * can fire that on the Netty thread. Clears session-scoped state without running ability
	 * {@code onDeactivate} (its gameplay side-effects would persist onto a leaving player).
	 */
	public static void onPlayerLeave(ServerPlayer player) {
		java.util.UUID id = player.getUUID();
		// ability cooldowns intentionally persist — they live on the player attachment (audit B5)
		io.github.grebeshok105.codex.core.resource.EnergyLocks.clear(id);
		if (HeroDataStore.get(player).hasHero()) {
			HeroDataStore.update(player, HeroData::clearActive);
		}
	}

	/**
	 * Drop every hero-scoped session-state bit for the player (swap/untransform).
	 * Subscribers register via {@link io.github.grebeshok105.codex.core.lifecycle.HeroLifecycle#onClear}
	 * (audit B23: transform used to clear a subset of what untransform cleared);
	 * ability cooldowns deliberately survive — persistent deadlines, not session state.
	 */
	public static void clearHeroRuntimeState(ServerPlayer player) {
		io.github.grebeshok105.codex.core.lifecycle.HeroLifecycle.fireClear(player);
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
