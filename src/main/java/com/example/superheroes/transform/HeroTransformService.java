package com.example.superheroes.transform;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.attachment.ModAttachments;
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
import java.util.UUID;
import java.util.WeakHashMap;

public final class HeroTransformService {
	private static final int COOLDOWN_TICKS = 20;
	private static final Map<UUID, Long> LAST_TRANSFORM_TICK = new WeakHashMap<>();

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
		HeroData updated = new HeroData(
				Optional.of(heroId),
				hero.getEnergyMax(),
				Math.min(data.mana(), hero.getManaMax()),
				bindings,
				java.util.Set.of()
		);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		com.example.superheroes.effect.RegulusTotemController.clear(player.getUUID());
		com.example.superheroes.effect.RegulusMadnessController.clearMadness(player);
		hero.applyPassives(player);
		player.refreshDimensions();
		player.setHealth(player.getMaxHealth());
		ModNetworking.syncHeroData(player, updated);
		ModNetworking.broadcastRemoteHeroSkin(player);
		playTransformFx(player, true);
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
		com.example.superheroes.effect.UnibeamController.clearState(player.getUUID());
		com.example.superheroes.effect.RegulusTotemController.clear(player.getUUID());
		com.example.superheroes.effect.RegulusMadnessController.clearMadness(player);
		com.example.superheroes.effect.ReinhardController.clearAdaptations(player);
		com.example.superheroes.effect.RaidenLifecycleController.clearOnUntransform(player);
		HeroData updated = data.withHero(null).withResources(0f, 0f).clearActive();
		player.setAttached(ModAttachments.HERO_DATA, updated);
		player.refreshDimensions();
		ModNetworking.syncHeroData(player, updated);
		ModNetworking.broadcastRemoteHeroSkin(player);
		if (playFx) {
			playTransformFx(player, false);
		}
		markTransformed(player);
		return true;
	}

	private static boolean isOnCooldown(ServerPlayer player) {
		Long last = LAST_TRANSFORM_TICK.get(player.getUUID());
		if (last == null) {
			return false;
		}
		return player.server.getTickCount() - last < COOLDOWN_TICKS;
	}

	private static void markTransformed(ServerPlayer player) {
		LAST_TRANSFORM_TICK.put(player.getUUID(), (long) player.server.getTickCount());
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero() && !data.activeAbilities().isEmpty()) {
			data = data.clearActive();
			player.setAttached(ModAttachments.HERO_DATA, data);
		}
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null) {
				hero.removePassives(player);
				hero.applyPassives(player);
			}
		}
		ModNetworking.syncHeroData(player, data);
	}

	public static void onPlayerRespawn(ServerPlayer newPlayer) {
		HeroData data = newPlayer.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero() && !data.activeAbilities().isEmpty()) {
			data = data.clearActive();
			newPlayer.setAttached(ModAttachments.HERO_DATA, data);
		}
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null) {
				hero.removePassives(newPlayer);
				hero.applyPassives(newPlayer);
			}
		}
		com.example.superheroes.effect.ReinhardController.onRespawn(newPlayer);
		ModNetworking.syncHeroData(newPlayer, data);
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		java.util.UUID id = player.getUUID();
		com.example.superheroes.ability.AbilityCooldowns.clear(id);
		com.example.superheroes.resource.EnergyLocks.clear(id);
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
