package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GokuSpiritBombAbility implements Ability {
	private static final int CHANNEL_TICKS = 80;
	private static final int COOLDOWN_TICKS = 900;
	private static final double RADIUS = 12.0;
	private static final float DAMAGE = 50f;
	private static final Map<UUID, ActiveSpiritBomb> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GOKU_SPIRIT_BOMB;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 180f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId()) && !ACTIVE.containsKey(player.getUUID());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ACTIVE.put(player.getUUID(), new ActiveSpiritBomb(player.position()));
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.6f, 0.8f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveSpiritBomb active = ACTIVE.get(player.getUUID());
		if (active == null) {
			return;
		}
		if (!active.tick(player)) {
			ACTIVE.remove(player.getUUID());
		}
	}

	private static final class ActiveSpiritBomb {
		private final Vec3 origin;
		private int ticks;

		private ActiveSpiritBomb(Vec3 origin) {
			this.origin = origin;
		}

		private boolean tick(ServerPlayer player) {
			if (!player.isAlive()) {
				return false;
			}
			ServerLevel level = player.serverLevel();
			player.setDeltaMovement(0, Math.max(player.getDeltaMovement().y, 0.02), 0);
			player.hurtMarked = true;
			Vec3 orb = player.position().add(0, 4.0, 0);
			double spread = 0.6 + (ticks / (double) CHANNEL_TICKS) * 2.2;
			level.sendParticles(ModParticles.GOKU_KI_AURA,
					orb.x, orb.y, orb.z, 18, spread, spread, spread, 0.08);
			level.sendParticles(ParticleTypes.END_ROD,
					orb.x, orb.y, orb.z, 10, spread, spread, spread, 0.04);
			ticks++;
			if (ticks < CHANNEL_TICKS) {
				return true;
			}
			Vec3 center = origin.add(player.getLookAngle().scale(8.0)).add(0, 1.2, 0);
			List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
					new AABB(center, center).inflate(RADIUS),
					e -> e != player && e.isAlive() && !e.isSpectator());
			for (LivingEntity victim : victims) {
				victim.invulnerableTime = 0;
				victim.hurt(ModDamageTypes.gokuSpiritBomb(level, player), DAMAGE);
				Vec3 push = victim.position().subtract(center).normalize().scale(1.4);
				victim.push(push.x, 0.7, push.z);
			}
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
					center.x, center.y, center.z, 2, 0.5, 0.5, 0.5, 0);
			level.sendParticles(ModParticles.GOKU_KAMEHAMEHA_CORE,
					center.x, center.y, center.z, 140, RADIUS * 0.25, 1.8, RADIUS * 0.25, 0.12);
			level.playSound(null, center.x, center.y, center.z,
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.7f);
			return false;
		}
	}
}
