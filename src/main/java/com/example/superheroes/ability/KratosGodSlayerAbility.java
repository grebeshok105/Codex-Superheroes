package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KratosGodSlayerAbility implements Ability {
	private static final int COOLDOWN_TICKS = 900;
	private static final double SCAN_RADIUS = 16.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KRATOS_GOD_SLAYER;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 200f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		LivingEntity target = null;
		double closest = Double.MAX_VALUE;
		AABB scan = player.getBoundingBox().inflate(SCAN_RADIUS);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, scan,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			double dist = le.distanceToSqr(player);
			if (dist < closest) {
				closest = dist;
				target = le;
			}
		}

		if (target == null) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
			return false;
		}

		Vec3 from = player.position();
		Vec3 to = target.position().subtract(target.getLookAngle().scale(1.5));
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				from.x, from.y + 1, from.z, 80, 0.5, 1.0, 0.5, 0.2);
		player.teleportTo(to.x, to.y, to.z);
		player.connection.resetPosition();
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				to.x, to.y + 1, to.z, 80, 0.5, 1.0, 0.5, 0.2);

		for (int i = 0; i < 5; i++) {
			float dmg = 12.0f;
			boolean lastHit = (i == 4);
			if (lastHit) {
				AABB aoe = target.getBoundingBox().inflate(4.0);
				for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, aoe,
						e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
					le.hurt(ModDamageTypes.kratosBlade(level, player), dmg);
					Vec3 push = le.position().subtract(target.position()).normalize().scale(1.2);
					le.setDeltaMovement(push.x, 0.4, push.z);
					le.hurtMarked = true;
				}
				level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
						target.getX(), target.getY() + 1, target.getZ(), 3, 1.0, 0.5, 1.0, 0.0);
			} else {
				target.hurt(ModDamageTypes.kratosBlade(level, player), dmg);
			}
			level.sendParticles(ParticleTypes.CRIT,
					target.getX(), target.getY() + 1, target.getZ(), 20, 0.6, 0.6, 0.6, 0.2);
			level.playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.8f + i * 0.1f);
		}

		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6f, 0.6f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
