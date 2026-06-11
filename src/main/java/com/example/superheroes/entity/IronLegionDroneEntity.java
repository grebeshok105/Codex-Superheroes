package com.example.superheroes.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Iron Legion — дроны-бойцы Железного Человека.
 *
 * Логика написана с нуля, без ванильных goal'ов и flying-навигации:
 * каждый тик дрон сам управляет своей скоростью (прямое steering-движение).
 *
 * Правила боя:
 *  - цель ВСЕГДА наследуется от владельца: кого владелец ударил последним
 *    (или кто ударил владельца) — того бьёт весь легион. Никаких «самовольных»
 *    целей вроде ближайшего крипера: своих целей дроны не выбирают вообще;
 *  - смена цели мгновенная: владелец ударил нового моба — дроны переключились;
 *  - высота под контролем: в бою дрон держится не выше ~3 блоков над целью,
 *    в патруле — не выше ~6 блоков над владельцем; вниз дрон идёт быстро
 *    (резкое пике), вверх — умеренно, поэтому «улететь в небо» он не может.
 */
public class IronLegionDroneEntity extends PathfinderMob {
	private static final int COMBAT_LIFETIME_TICKS = 600; // 30 seconds
	private static final int RETREAT_TICKS = 80;          // 4 seconds to fly away
	private static final double RETREAT_SPEED_Y = 0.8;

	// steering
	private static final double COMBAT_SPEED = 0.45;   // блоков/тик к точке боя
	private static final double ESCORT_SPEED = 0.33;   // блоков/тик в патруле
	private static final double MAX_RISE = 0.30;       // вверх — умеренно
	private static final double MAX_FALL = -0.75;      // вниз — стремительно
	private static final double COMBAT_CEILING = 3.0;  // не выше стольких блоков над целью
	private static final double ESCORT_CEILING = 6.0;  // не выше стольких блоков над владельцем
	private static final double TARGET_RANGE = 48.0;

	private static final EntityDataAccessor<Integer> DATA_SUIT_VARIANT =
			SynchedEntityData.defineId(IronLegionDroneEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
			SynchedEntityData.defineId(IronLegionDroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> DATA_RETREATING =
			SynchedEntityData.defineId(IronLegionDroneEntity.class, EntityDataSerializers.BOOLEAN);

	private int lifeTicks;
	private int attackCooldown;
	private int repathTicks;
	private int diveTicks;
	private Vec3 waypoint = Vec3.ZERO;

	public IronLegionDroneEntity(EntityType<? extends IronLegionDroneEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.xpReward = 0;
		this.lifeTicks = COMBAT_LIFETIME_TICKS + RETREAT_TICKS;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.5)
				.add(Attributes.FLYING_SPEED, 1.5)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SUIT_VARIANT, 0);
		builder.define(DATA_OWNER, Optional.empty());
		builder.define(DATA_RETREATING, false);
	}

	@Override
	protected void registerGoals() {
		// Никаких ванильных goal'ов: всё движение и выбор цели — в tick().
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}

		lifeTicks--;
		if (lifeTicks <= RETREAT_TICKS && !isRetreating()) {
			startRetreat();
		}
		if (lifeTicks <= 0) {
			discard();
			return;
		}
		if (isRetreating()) {
			setDeltaMovement(getDeltaMovement().x * 0.5, RETREAT_SPEED_Y, getDeltaMovement().z * 0.5);
			if (level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						getX(), getY() + 0.5, getZ(), 3, 0.2, 0.3, 0.2, 0.02);
			}
			return;
		}

		Player owner = superheroes$owner();
		LivingEntity target = superheroes$resolveTarget(owner);
		setTarget(target);
		if (attackCooldown > 0) {
			attackCooldown--;
		}

		if (target != null) {
			superheroes$combatTick(target);
		} else {
			superheroes$escortTick(owner);
		}
	}

	/**
	 * Единственный источник целей — бой владельца: кто ударил владельца или кого
	 * владелец ударил последним. Если владельца не трогают — добиваем того, кто
	 * посмел ударить самого дрона. Ближайших мобов дроны НЕ выбирают сами.
	 */
	@Nullable
	private LivingEntity superheroes$resolveTarget(@Nullable Player owner) {
		if (owner != null) {
			LivingEntity hurtBy = owner.getLastHurtByMob();
			if (superheroes$validTarget(hurtBy, owner)) {
				return hurtBy;
			}
			LivingEntity hurt = owner.getLastHurtMob();
			if (superheroes$validTarget(hurt, owner)) {
				return hurt;
			}
		}
		LivingEntity revenge = getLastHurtByMob();
		if (superheroes$validTarget(revenge, owner)) {
			return revenge;
		}
		return null;
	}

	private boolean superheroes$validTarget(@Nullable LivingEntity candidate, @Nullable Player owner) {
		return candidate != null && candidate.isAlive()
				&& !(candidate instanceof IronLegionDroneEntity)
				&& (owner == null || !candidate.getUUID().equals(owner.getUUID()))
				&& distanceTo(candidate) <= TARGET_RANGE;
	}

	/** Бой: хаотичные витки вокруг цели на малой высоте + резкие пике с ударом. */
	private void superheroes$combatTick(LivingEntity target) {
		getLookControl().setLookAt(target, 30.0f, 30.0f);

		if (diveTicks > 0) {
			diveTicks--;
			superheroes$steer(target.position().add(0, target.getBbHeight() * 0.5, 0), COMBAT_SPEED * 1.25);
		} else {
			if (--repathTicks <= 0) {
				repathTicks = 4 + random.nextInt(5);
				if (random.nextFloat() < 0.45f) {
					diveTicks = 14;
				} else {
					double angle = random.nextDouble() * Math.PI * 2;
					double dist = 2.0 + random.nextDouble() * 5.0;
					double yOff = 0.5 + random.nextDouble() * 2.0;
					waypoint = new Vec3(
							target.getX() + Math.cos(angle) * dist,
							target.getY() + yOff,
							target.getZ() + Math.sin(angle) * dist);
				}
			}
			if (waypoint != Vec3.ZERO) {
				superheroes$steer(waypoint, COMBAT_SPEED);
			}
		}

		superheroes$enforceCeiling(target.getY() + COMBAT_CEILING);

		if (attackCooldown <= 0 && distanceToSqr(target) < 6.5) {
			doHurtTarget(target);
			attackCooldown = 14;
			diveTicks = 0;
			repathTicks = 0;
		}
	}

	/** Патруль: шустрые броски вокруг владельца на небольшой высоте. */
	private void superheroes$escortTick(@Nullable Player owner) {
		if (owner == null) {
			// владельца нет — мягко зависаем и медленно снижаемся
			Vec3 v = getDeltaMovement();
			setDeltaMovement(v.x * 0.8, Math.max(v.y * 0.8, -0.08), v.z * 0.8);
			return;
		}
		if (--repathTicks <= 0 || waypoint == Vec3.ZERO) {
			repathTicks = 6 + random.nextInt(8);
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = 4.0 + random.nextDouble() * 12.0;
			double yOff = 1.0 + random.nextDouble() * 3.5;
			waypoint = new Vec3(
					owner.getX() + Math.cos(angle) * dist,
					owner.getY() + yOff,
					owner.getZ() + Math.sin(angle) * dist);
		}
		superheroes$steer(waypoint, ESCORT_SPEED);
		superheroes$enforceCeiling(owner.getY() + ESCORT_CEILING);
	}

	/** Прямое управление скоростью: плавный доворот к точке, без pathfinding'а. */
	private void superheroes$steer(Vec3 desired, double speed) {
		Vec3 dir = desired.subtract(position());
		double len = dir.length();
		if (len < 0.2) {
			return;
		}
		Vec3 want = dir.scale(speed / len);
		Vec3 v = getDeltaMovement().scale(0.55).add(want.scale(0.6));
		double vy = Mth.clamp(v.y, MAX_FALL, MAX_RISE);
		setDeltaMovement(v.x, vy, v.z);
		// корпус — по направлению движения
		if (v.horizontalDistanceSqr() > 1.0e-4) {
			float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0f;
			setYRot(yaw);
			yBodyRot = yaw;
		}
	}

	/** Жёсткий потолок: выше нельзя — мгновенно идём в снижение. */
	private void superheroes$enforceCeiling(double maxY) {
		if (getY() > maxY) {
			Vec3 v = getDeltaMovement();
			setDeltaMovement(v.x, Math.min(v.y, -0.5), v.z);
			// и точку назначения тоже опускаем, чтобы не тянуло обратно вверх
			if (waypoint != Vec3.ZERO && waypoint.y > maxY) {
				waypoint = new Vec3(waypoint.x, maxY - 1.0, waypoint.z);
			}
		}
	}

	private void startRetreat() {
		entityData.set(DATA_RETREATING, true);
		setTarget(null);
	}

	public boolean isRetreating() {
		return entityData.get(DATA_RETREATING);
	}

	public int getSuitVariant() {
		return entityData.get(DATA_SUIT_VARIANT);
	}

	public void setSuitVariant(int variant) {
		entityData.set(DATA_SUIT_VARIANT, variant);
	}

	@Nullable
	public UUID getOwnerUuid() {
		return entityData.get(DATA_OWNER).orElse(null);
	}

	public void setOwnerUuid(@Nullable UUID uuid) {
		entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// Летающая машина не бьётся об землю, стены и не давится в толпе —
		// иначе на бешеной скорости дроны сами себя избивают со звуком голема.
		if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING)) {
			return false;
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof Player p && p.getUUID().equals(getOwnerUuid())) {
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
			net.minecraft.core.BlockPos pos) {
		// no-op: дроны не регистрируют падение вовсе
	}

	@Override
	public boolean shouldShowName() {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("SuitVariant", getSuitVariant());
		tag.putInt("LifeTicks", lifeTicks);
		if (getOwnerUuid() != null) tag.putUUID("Owner", getOwnerUuid());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("SuitVariant")) setSuitVariant(tag.getInt("SuitVariant"));
		if (tag.contains("LifeTicks")) lifeTicks = tag.getInt("LifeTicks");
		if (tag.hasUUID("Owner")) setOwnerUuid(tag.getUUID("Owner"));
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSq) {
		return false;
	}

	@Nullable
	private Player superheroes$owner() {
		UUID uuid = getOwnerUuid();
		return uuid == null ? null : level().getPlayerByUUID(uuid);
	}
}
