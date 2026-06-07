package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegulusTotemController {
	private static final Set<UUID> TOTEM_USED = ConcurrentHashMap.newKeySet();

	private RegulusTotemController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.hasHero() || !RegulusHero.ID.equals(data.heroId())) {
				return true;
			}
			if (TOTEM_USED.contains(player.getUUID())) {
				if (RegulusMadnessController.consumeBonusLife(player)) {
					player.setHealth(player.getMaxHealth() * 0.5f);
					player.removeAllEffects();
					player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
					player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
					player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
					return false;
				}
				return true;
			}
			TOTEM_USED.add(player.getUUID());
			player.setHealth(player.getMaxHealth());
			player.removeAllEffects();
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
					player.getX(), player.getY() + 1.0, player.getZ(),
					60, 0.4, 0.6, 0.4, 0.3);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1f);
			return false;
		});
	}

	public static void clear(UUID playerId) {
		TOTEM_USED.remove(playerId);
	}

	public static boolean wasUsed(UUID playerId) {
		return TOTEM_USED.contains(playerId);
	}
}
