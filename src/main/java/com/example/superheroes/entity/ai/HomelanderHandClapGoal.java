package com.example.superheroes.entity.ai;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class HomelanderHandClapGoal extends Goal {
	private static final double RANGE = 18.0;
	private static final double CONE_HALF_ANGLE_COS = Math.cos(Math.toRadians(45.0));
	private static final float DAMAGE = 18.0f;
	private static final double KNOCKBACK = 3.5;
	private static final int CHARGE_TICKS = 16;
	private static final int RECOVER_TICKS = 12;
	private static final int CD_AFTER = 200;

	private final HomelanderBossEntity boss;
	private int phaseTick;
	private boolean fired;

	public HomelanderHandClapGoal(HomelanderBossEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = boss.getTarget();
		if (target == null || !target.isAlive()) return false;
		if (boss.getHandClapCooldown() > 0) return false;
		double d2 = boss.distanceToSqr(target);
		return d2 <= RANGE * RANGE && d2 >= 4.0;
	}

	@Override
	public boolean canContinueToUse() {
		return phaseTick < CHARGE_TICKS + RECOVER_TICKS && boss.getTarget() != null;
	}

	@Override
	public void start() {
		phaseTick = 0;
		fired = false;
		ServerLevel sl = (ServerLevel) boss.level();
		sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
				SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.0f, 0.7f);
	}

	@Override
	public void stop() {
		boss.setHandClapCooldown(CD_AFTER);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = boss.getTarget();
		if (target == null) return;
		boss.getLookControl().setLookAt(target, 30f, 30f);
		ServerLevel sl = (ServerLevel) boss.level();
		if (phaseTick < CHARGE_TICKS) {
			Vec3 c = boss.position().add(0, boss.getBbHeight() * 0.65, 0);
			sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 4, 0.3, 0.2, 0.3, 0.02);
		}
		if (!fired && phaseTick == CHARGE_TICKS) {
			fired = true;
			Vec3 origin = boss.position().add(0, boss.getBbHeight() * 0.7, 0);
			Vec3 forward = boss.getViewVector(1f).normalize();
			AABB area = new AABB(origin, origin).inflate(RANGE);
			List<Entity> hits = sl.getEntities(boss, area);
			DamageSource ds = ModDamageTypes.homelanderHandClap(sl, boss);
			for (Entity e : hits) {
				if (!(e instanceof LivingEntity le) || !le.isAlive() || e == boss) continue;
				Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
				double d = to.length();
				if (d > RANGE || d < 0.001) continue;
				Vec3 dir = to.scale(1.0 / d);
				if (dir.dot(forward) < CONE_HALF_ANGLE_COS) continue;
				le.hurt(ds, DAMAGE);
				Vec3 push = forward.scale(KNOCKBACK);
				e.push(push.x, 0.6, push.z);
				e.hurtMarked = true;
			}

			Vec3 tip = origin.add(forward.scale(2.5));
			sl.sendParticles(ParticleTypes.EXPLOSION, tip.x, tip.y, tip.z, 2, 0.4, 0.4, 0.4, 0.0);
			for (int i = 1; i <= 12; i++) {
				double t = i;
				double spread = 0.4 + i * 0.25;
				Vec3 cc = origin.add(forward.scale(t * 1.4));
				sl.sendParticles(ParticleTypes.CLOUD, cc.x, cc.y, cc.z, 8, spread, 0.5, spread, 0.05);
				sl.sendParticles(ParticleTypes.LARGE_SMOKE, cc.x, cc.y, cc.z, 4, spread * 0.5, 0.3, spread * 0.5, 0.03);
			}
			sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
					ModSounds.HOMELANDER_HAND_CLAP, SoundSource.HOSTILE, 2.0f, 0.95f);
			sl.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
					SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.6f, 1.1f);

			for (ServerPlayer nearby : PlayerLookup.around(sl, boss.position(), 24.0)) {
				double dist = nearby.position().distanceTo(boss.position());
				float intensity = (float) Math.max(0.0, 1.0 - dist / 24.0) * 1.8f;
				if (intensity > 0.05f) {
					ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, 16));
				}
			}
		}
		phaseTick++;
	}
}
