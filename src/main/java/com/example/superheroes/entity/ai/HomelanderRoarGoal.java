package com.example.superheroes.entity.ai;

import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class HomelanderRoarGoal extends Goal {
	private static final int CHARGE_TICKS = 14;
	private static final int CD_AFTER = 220;
	private static final double TRIGGER_RANGE = 14.0;
	private static final double EFFECT_RADIUS = 14.0;
	private static final float DAMAGE = 7.0f;

	private final HomelanderBossEntity boss;
	private int phaseTick;
	private boolean fired;

	public HomelanderRoarGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity t = boss.getTarget();
		if (t == null || !t.isAlive()) return false;
		if (boss.getRoarCooldown() > 0) return false;
		return boss.distanceToSqr(t) <= TRIGGER_RANGE * TRIGGER_RANGE;
	}

	@Override
	public boolean canContinueToUse() {
		return phaseTick <= CHARGE_TICKS && !fired;
	}

	@Override
	public void start() {
		phaseTick = 0;
		fired = false;
		ServerLevel sl = (ServerLevel) boss.level();
		sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
				ModSounds.HOMELANDER_ROAR_DEEP, SoundSource.HOSTILE, 1.8f, 0.95f);
	}

	@Override
	public void stop() {
		boss.setRoarCooldown(CD_AFTER);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ServerLevel sl = (ServerLevel) boss.level();
		Vec3 c = boss.position();
		if (phaseTick < CHARGE_TICKS) {
			sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					c.x, c.y + 1.4, c.z,
					6, 0.6, 0.2, 0.6, 0.04);
		} else if (!fired) {
			fired = true;
			AABB box = boss.getBoundingBox().inflate(EFFECT_RADIUS);
			List<Entity> nearby = sl.getEntities(boss, box);
			DamageSource ds = com.example.superheroes.damage.ModDamageTypes.homelanderRoarBoss((net.minecraft.server.level.ServerLevel) boss.level(), boss);
			for (Entity e : nearby) {
				if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
				if (boss.distanceToSqr(e) > EFFECT_RADIUS * EFFECT_RADIUS) continue;
				le.hurt(ds, DAMAGE);
				Vec3 push = e.position().subtract(c).normalize().scale(1.3).add(0, 0.5, 0);
				e.push(push.x, push.y, push.z);
				e.hurtMarked = true;
				if (e instanceof Player) {
					le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
					le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
					le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
				}
			}
			sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1.0, c.z,
					4, 0.8, 0.2, 0.8, 0.0);
			sl.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1.0, c.z,
					1, 0.0, 0.0, 0.0, 0.0);
			sl.playSound(null, c.x, c.y, c.z,
					ModSounds.HOMELANDER_ROAR, SoundSource.HOSTILE, 1.8f, 0.85f);
			sl.playSound(null, c.x, c.y, c.z,
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0f, 0.7f);
		}
		phaseTick++;
	}
}
