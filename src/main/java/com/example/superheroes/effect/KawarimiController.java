package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.NarutoHero;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Naruto passive: Substitution Jutsu.
 * On lethal damage, the player teleports behind the attacker, debuffs the attacker,
 * and is restored to 30% HP. 60s cooldown.
 */
public final class KawarimiController {
	private static final long COOLDOWN_TICKS = 1200L;

	private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

	private KawarimiController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) return true;
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.hasHero() || !NarutoHero.ID.equals(data.heroId())) return true;
			float hp = player.getHealth();
			if (amount < hp) return true;
			long now = player.serverLevel().getGameTime();
			Long last = LAST_TRIGGER.get(player.getUUID());
			if (last != null && now - last < COOLDOWN_TICKS) return true;
			Entity attacker = source.getEntity();
			if (attacker == null) return true;

			LAST_TRIGGER.put(player.getUUID(), now);
			triggerSubstitution(player, attacker);
			return false;
		});
	}

	public static long getCooldownRemainingTicks(ServerPlayer player) {
		Long last = LAST_TRIGGER.get(player.getUUID());
		if (last == null) return 0;
		long elapsed = player.serverLevel().getGameTime() - last;
		return Math.max(0, COOLDOWN_TICKS - elapsed);
	}

	public static void onPlayerDisconnect(UUID id) {
		LAST_TRIGGER.remove(id);
	}

	private static void triggerSubstitution(ServerPlayer player, Entity attacker) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();

		// Spawn falling log decoy at original position
		level.sendParticles(ModParticles.NARUTO_KAWARIMI_SMOKE,
				origin.x, origin.y + 1.0, origin.z, 40, 0.6, 1.0, 0.6, 0.06);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				origin.x, origin.y + 1.0, origin.z, 50, 0.7, 1.0, 0.7, 0.04);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.4f, 0.8f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.FOX_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);

		// Teleport behind attacker
		Vec3 attackerLook = attacker.getLookAngle();
		Vec3 dest = attacker.position().subtract(attackerLook.scale(2.0));
		dest = new Vec3(dest.x, attacker.getY(), dest.z);
		player.connection.teleport(dest.x, dest.y, dest.z, attacker.getYRot(), 0, Set.of());

		// Debuff attacker
		if (attacker instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true, true));
			living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true, true));
		}

		// Restore to 30% HP
		float maxHealth = player.getMaxHealth();
		float restored = Math.max(maxHealth * 0.3f, 1.0f);
		player.setHealth(restored);

		level.sendParticles(ModParticles.NARUTO_KAWARIMI_SMOKE,
				dest.x, dest.y + 1.0, dest.z, 30, 0.6, 1.0, 0.6, 0.06);
		level.sendParticles(ParticleTypes.CLOUD,
				dest.x, dest.y + 1.0, dest.z, 30, 0.5, 0.8, 0.5, 0.06);
		level.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.FOX_TELEPORT, SoundSource.PLAYERS, 1.4f, 1.2f);
		level.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.6f);
	}
}
