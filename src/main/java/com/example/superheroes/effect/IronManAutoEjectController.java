package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IronManAutoEjectController {
	private static final float HP_THRESHOLD = 0.20f;
	private static final long COOLDOWN_TICKS = 20L * 60L; // 60 seconds
	private static final double EJECT_VELOCITY = 1.18;    // ~8 blocks of vertical lift after gravity/drag

	private static final Map<UUID, Long> nextAvailableTick = new HashMap<>();

	private IronManAutoEjectController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player, now);
			}
		});
	}

	private static void tick(ServerPlayer player, long now) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !IronManHero.ID.equals(data.heroId())) {
			return;
		}
		if (!player.isAlive() || player.getHealth() <= 0f) {
			return;
		}
		float maxHp = player.getMaxHealth();
		if (maxHp <= 0f) {
			return;
		}
		if (player.getHealth() / maxHp >= HP_THRESHOLD) {
			return;
		}
		Long next = nextAvailableTick.get(player.getUUID());
		if (next != null && now < next) {
			return;
		}
		fire(player);
		nextAvailableTick.put(player.getUUID(), now + COOLDOWN_TICKS);
	}

	private static void fire(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();

		Vec3 v = player.getDeltaMovement();
		player.setDeltaMovement(v.x, EJECT_VELOCITY, v.z);
		player.hurtMarked = true;
		player.fallDistance = 0f;

		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true, true));

		level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 0.2, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 60, 0.6, 0.05, 0.6, 0.15);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.1, pos.z, 24, 0.7, 0.05, 0.7, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, 30, 0.4, 0.05, 0.4, 0.2);

		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.9f, 0.7f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.4f);
	}
}
