package com.example.superheroes.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shield Dash — Капитан Америка делает рывок щитом вперёд (8 блоков), сбивая
 * первого встречного на пути: 12 урона, нокбек, Slowness II на 3 секунды.
 * PvP-инструмент: позволяет догнать летающего/убегающего врага и сбить с ритма.
 */
public final class CapShieldDashAbility implements Ability {
	private static final int COOLDOWN_TICKS = 200; // 10s
	private static final double DASH_BLOCKS = 8.0;
	private static final double HIT_RADIUS = 1.6;
	private static final float DAMAGE = 12.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.CAP_SHIELD_DASH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 60f;
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
		Vec3 look = player.getLookAngle().normalize();
		// Импульс рывка
		Vec3 push = new Vec3(look.x * 1.6, Math.max(0.05, look.y * 0.6), look.z * 1.6);
		player.setDeltaMovement(push);
		player.hurtMarked = true;
		player.fallDistance = 0f;

		Vec3 from = player.position();
		Vec3 to = from.add(look.scale(DASH_BLOCKS));
		AABB sweep = new AABB(from, to).inflate(HIT_RADIUS);

		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, sweep,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.isCreative()));
		LivingEntity hit = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity t : targets) {
			double d = t.position().distanceToSqr(from);
			if (d < bestDist) {
				bestDist = d;
				hit = t;
			}
		}
		if (hit != null) {
			hit.hurt(player.damageSources().playerAttack(player), DAMAGE);
			hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false, true));
			Vec3 kb = look.scale(1.4);
			hit.push(kb.x, 0.4, kb.z);
			hit.hurtMarked = true;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.getX(), hit.getY() + hit.getBbHeight() * 0.5, hit.getZ(), 1, 0, 0, 0, 0);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.4f, 0.7f);
		level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(),
				14, 0.4, 0.4, 0.4, 0.2);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
