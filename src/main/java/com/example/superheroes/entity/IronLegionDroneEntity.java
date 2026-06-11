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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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
		this.moveControl = new FlyingMoveControl(this, 30, true);
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
				.add(Attributes.MOVEMENT_SPEED, 0.35)
				.add(Attributes.FLYING_SPEED, 0.55)
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
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;

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
		Entity attacker = source.getEntity();
		if (attacker instanceof Player p && p.getUUID().equals(getOwnerUuid())) {
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	protected boolean shouldShowName() {
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
}
