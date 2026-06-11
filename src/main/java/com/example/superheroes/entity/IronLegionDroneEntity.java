package com.example.superheroes.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Iron Legion — дроны-бойцы Железного Человека.
 * Каждый носит уникальный скин костюма, атакует враждебных мобов,
 * через 30 секунд улетает вверх и деспавнится.
 */
public class IronLegionDroneEntity extends PathfinderMob {
	private static final int COMBAT_LIFETIME_TICKS = 600; // 30 seconds
	private static final int RETREAT_TICKS = 80;          // 4 seconds to fly away
	private static final double RETREAT_SPEED_Y = 0.8;

	private static final EntityDataAccessor<Integer> DATA_SUIT_VARIANT =
			SynchedEntityData.defineId(IronLegionDroneEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
			SynchedEntityData.defineId(IronLegionDroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> DATA_RETREATING =
			SynchedEntityData.defineId(IronLegionDroneEntity.class, EntityDataSerializers.BOOLEAN);

	private int lifeTicks;

	public IronLegionDroneEntity(EntityType<? extends IronLegionDroneEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 85, true);
		this.setNoGravity(true);
		this.xpReward = 0;
		this.lifeTicks = COMBAT_LIFETIME_TICKS + RETREAT_TICKS;
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
		nav.setCanOpenDoors(false);
		nav.setCanFloat(true);
		nav.setCanPassDoors(true);
		return nav;
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
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new SwarmAttackGoal());
		this.goalSelector.addGoal(4, new WildRoamGoal());
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;

		// Inherit the owner's fights: attack whoever hurts the owner or whoever the owner attacks
		if (!isRetreating() && tickCount % 10 == 0 && getTarget() == null) {
			LivingEntity ownerTarget = superheroes$ownerCombatTarget();
			if (ownerTarget != null && ownerTarget.isAlive() && distanceTo(ownerTarget) < 48
					&& !(ownerTarget instanceof IronLegionDroneEntity)) {
				setTarget(ownerTarget);
			}
		}

		lifeTicks--;
		if (lifeTicks <= RETREAT_TICKS && !isRetreating()) {
			startRetreat();
		}
		if (isRetreating()) {
			setDeltaMovement(getDeltaMovement().x * 0.5, RETREAT_SPEED_Y, getDeltaMovement().z * 0.5);
			if (level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						getX(), getY() + 0.5, getZ(), 3, 0.2, 0.3, 0.2, 0.02);
			}
		}
		if (lifeTicks <= 0) {
			discard();
		}
	}

	private void startRetreat() {
		entityData.set(DATA_RETREATING, true);
		setTarget(null);
		getNavigation().stop();
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

	@Nullable
	private LivingEntity superheroes$ownerCombatTarget() {
		Player owner = superheroes$owner();
		if (owner == null) return null;
		LivingEntity hurtBy = owner.getLastHurtByMob();
		if (hurtBy != null && hurtBy.isAlive() && !hurtBy.getUUID().equals(owner.getUUID())) {
			return hurtBy;
		}
		LivingEntity hurt = owner.getLastHurtMob();
		if (hurt != null && hurt.isAlive() && !hurt.getUUID().equals(owner.getUUID())) {
			return hurt;
		}
		return null;
	}

	/**
	 * Рой-атака: дроны хаотично носятся ВОКРУГ ВРАГА на разных высотах и
	 * постоянно пикируют на него с ударами — изводят цель со всех сторон.
	 */
	private class SwarmAttackGoal extends net.minecraft.world.entity.ai.goal.Goal {
		private int repathCooldown;
		private int attackCooldown;
		private boolean diving;

		SwarmAttackGoal() {
			setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return !isRetreating() && target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void stop() {
			diving = false;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) return;
			getLookControl().setLookAt(target, 30.0f, 30.0f);
			if (attackCooldown > 0) attackCooldown--;

			double distSq = distanceToSqr(target);
			if (diving) {
				// Пикируем прямо в цель
				getNavigation().moveTo(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 2.6);
				if (distSq < 6.5) {
					if (attackCooldown <= 0) {
						doHurtTarget(target);
						attackCooldown = 14;
					}
					diving = false;
					repathCooldown = 0;
				}
				return;
			}

			if (repathCooldown-- > 0) return;
			repathCooldown = 3 + random.nextInt(5);

			// Каждые ~0.2с — либо новый хаотичный виток вокруг врага, либо пике
			if (random.nextFloat() < 0.45f) {
				diving = true;
				return;
			}
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = 2.0 + random.nextDouble() * 6.0;
			double yOffset = 0.5 + random.nextDouble() * 5.0;
			getNavigation().moveTo(
					target.getX() + Math.cos(angle) * dist,
					target.getY() + yOffset,
					target.getZ() + Math.sin(angle) * dist,
					2.4);
		}
	}

	/**
	 * Без цели дроны не висят на месте, а бешено носятся по округе в поисках
	 * драки (хаотичные точки в радиусе от владельца).
	 */
	private class WildRoamGoal extends net.minecraft.world.entity.ai.goal.Goal {
		private int repathCooldown;

		WildRoamGoal() {
			setFlags(java.util.EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !isRetreating() && getTarget() == null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			if (repathCooldown-- > 0) return;
			repathCooldown = 3 + random.nextInt(5);
			Player owner = superheroes$owner();
			double cx = owner != null ? owner.getX() : getX();
			double cy = owner != null ? owner.getY() : getY();
			double cz = owner != null ? owner.getZ() : getZ();
			// Бешеный патруль: хаотичные точки в радиусе до 35 блоков от владельца,
			// на любой высоте — дроны проносятся мимо как сорвавшиеся с цепи
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = 6.0 + random.nextDouble() * 29.0;
			getNavigation().moveTo(
					cx + Math.cos(angle) * dist,
					cy + 1.0 + random.nextDouble() * 14.0,
					cz + Math.sin(angle) * dist,
					2.8);
		}
	}
}
