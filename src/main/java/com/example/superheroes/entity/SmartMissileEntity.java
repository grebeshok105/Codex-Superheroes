package com.example.superheroes.entity;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Самонаводящаяся нано-ракета Железного Человека. Летит к цели, плавно
 * доворачивая вектор скорости (ограниченная угловая скорость), при попадании
 * детонирует с уроном по площади и отбросом — без разрушения блоков. Яркий
 * дымно-огненный след. Запускается залпом из {@link com.example.superheroes.ability.SmartMissileAbility}.
 */
public class SmartMissileEntity extends Projectile {
	private static final double SPEED = 0.95;
	private static final double TURN = 0.22;
	private static final int MAX_LIFETIME = 90;
	private static final float DIRECT_DAMAGE = 7.0f;
	private static final float AOE_DAMAGE = 9.0f;
	private static final double AOE_RADIUS = 3.0;
	private static final double ACQUIRE_RANGE = 24.0;

	private int lifeTicks = 0;
	private UUID targetUuid;

	public SmartMissileEntity(EntityType<? extends SmartMissileEntity> type, Level level) {
		super(type, level);
	}

	public static SmartMissileEntity launch(LivingEntity owner, Level level, Vec3 spawnPos, Vec3 dir, LivingEntity target) {
		SmartMissileEntity m = new SmartMissileEntity(ModEntities.SMART_MISSILE, level);
		m.setOwner(owner);
		m.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
		m.setDeltaMovement(dir.normalize().scale(SPEED));
		if (target != null) {
			m.targetUuid = target.getUUID();
		}
		return m;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 pos = this.position();
		Vec3 motion = this.getDeltaMovement();

		if (this.level() instanceof ServerLevel server) {
			lifeTicks++;
			if (lifeTicks > MAX_LIFETIME) {
				detonate(server, pos, null);
				return;
			}
			LivingEntity target = resolveTarget(server);
			if (target != null) {
				Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(pos).normalize();
				Vec3 cur = motion.normalize();
				Vec3 blended = cur.add(aim.subtract(cur).scale(TURN)).normalize().scale(SPEED);
				motion = blended;
				this.setDeltaMovement(motion);
				if (target.position().add(0, target.getBbHeight() * 0.5, 0).distanceTo(pos) < 1.3) {
					detonate(server, pos, target);
					return;
				}
			}
			// прямое столкновение с любой живностью
			AABB hitBox = this.getBoundingBox().expandTowards(motion).inflate(0.3);
			for (LivingEntity le : server.getEntitiesOfClass(LivingEntity.class, hitBox,
					e -> e.isAlive() && !e.isSpectator() && e != this.getOwner())) {
				detonate(server, le.position().add(0, le.getBbHeight() * 0.5, 0), le);
				return;
			}
		}

		this.setPos(pos.add(motion));
		// поворот модели/следа по вектору
		double horiz = motion.horizontalDistance();
		this.setYRot((float) (Math.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Math.atan2(motion.y, horiz) * (180.0 / Math.PI)));

		if (this.level().isClientSide) {
			Level level = this.level();
			level.addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
			level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0.01, 0);
			if (this.tickCount % 2 == 0) {
				level.addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
			}
		}
	}

	private LivingEntity resolveTarget(ServerLevel server) {
		if (targetUuid != null) {
			Entity e = server.getEntity(targetUuid);
			if (e instanceof LivingEntity le && le.isAlive()) {
				return le;
			}
			targetUuid = null;
		}
		// перезахват ближайшей цели
		LivingEntity best = null;
		double bestDist = ACQUIRE_RANGE * ACQUIRE_RANGE;
		for (LivingEntity le : server.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(ACQUIRE_RANGE),
				e -> e.isAlive() && !e.isSpectator() && e != this.getOwner())) {
			double d = le.distanceToSqr(this);
			if (d < bestDist) {
				bestDist = d;
				best = le;
			}
		}
		if (best != null) {
			targetUuid = best.getUUID();
		}
		return best;
	}

	private void detonate(ServerLevel server, Vec3 at, LivingEntity direct) {
		LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
		if (direct != null) {
			direct.hurt(owner != null ? ModDamageTypes.repulsor(server, owner) : server.damageSources().explosion(this, owner),
					DIRECT_DAMAGE);
		}
		List<LivingEntity> nearby = server.getEntitiesOfClass(LivingEntity.class,
				new AABB(at, at).inflate(AOE_RADIUS),
				e -> e.isAlive() && !e.isSpectator() && e != this.getOwner());
		for (LivingEntity e : nearby) {
			double dist = e.position().distanceTo(at);
			float falloff = (float) Math.max(0.2, 1.0 - dist / (AOE_RADIUS + 1.0));
			e.hurt(owner != null ? ModDamageTypes.repulsor(server, owner) : server.damageSources().explosion(this, owner),
					AOE_DAMAGE * falloff);
			Vec3 away = e.position().subtract(at).normalize().scale(0.8 * falloff);
			e.push(away.x, 0.35 * falloff, away.z);
			e.hurtMarked = true;
		}
		server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, 0.3, 0.3, 0.3, 0.0);
		server.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0.0);
		server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 14, 0.5, 0.5, 0.5, 0.05);
		server.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.08);
		server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.2f);
		this.discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (this.level() instanceof ServerLevel server) {
			detonate(server, result.getLocation(), null);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return entity != this.getOwner() && super.canHitEntity(entity);
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("LifeTicks", lifeTicks);
		if (targetUuid != null) {
			tag.putUUID("Target", targetUuid);
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		lifeTicks = tag.getInt("LifeTicks");
		if (tag.hasUUID("Target")) {
			targetUuid = tag.getUUID("Target");
		}
	}
}
