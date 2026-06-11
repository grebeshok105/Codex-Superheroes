package com.example.superheroes.entity;

import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Naruto Kage Bunshin (decoy clone).
 *  - PathfinderMob (not Monster) so passive mobs don't auto-aggro.
 *  - 1 HP — dies on first hit (POOF particles).
 *  - Despawns after configured lifetime.
 *  - No combat AI; only stands around looking idle.
 */
public class KageBunshinEntity extends PathfinderMob {
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
			SynchedEntityData.defineId(KageBunshinEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private int lifeTicks = 500;

	public KageBunshinEntity(EntityType<? extends KageBunshinEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.FOLLOW_RANGE, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER, Optional.empty());
	}

	public void setOwnerUuid(@Nullable UUID owner) {
		this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
	}

	@Nullable
	public UUID getOwnerUuid() {
		return this.entityData.get(DATA_OWNER).orElse(null);
	}

	public void setLifetimeTicks(int ticks) {
		this.lifeTicks = ticks;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) return false;
		if (source.getEntity() != null && source.getEntity().getUUID().equals(getOwnerUuid())) {
			return false;
		}
		this.poof();
		this.discard();
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			if (this.tickCount % 6 == 0) {
				this.level().addParticle(ParticleTypes.SMOKE,
						this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
						this.getY() + this.random.nextDouble() * 1.5,
						this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
						0, 0.02, 0);
			}
			return;
		}
		this.lifeTicks--;
		if (this.lifeTicks <= 0) {
			this.poof();
			this.discard();
		}
	}

	private void poof() {
		if (!(this.level() instanceof ServerLevel level)) return;
		level.sendParticles(ModParticles.NARUTO_CLONE_POOF,
				this.getX(), this.getY() + 1.0, this.getZ(), 18, 0.4, 0.6, 0.4, 0.05);
		level.sendParticles(ParticleTypes.CLOUD,
				this.getX(), this.getY() + 1.0, this.getZ(), 22, 0.4, 0.6, 0.4, 0.05);
		level.sendParticles(ParticleTypes.WHITE_ASH,
				this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.5, 0.5, 0.5, 0.05);
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.ALLAY_HURT, SoundSource.HOSTILE, 0.7f, 1.4f);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("LifeTicks", this.lifeTicks);
		UUID owner = getOwnerUuid();
		if (owner != null) tag.putUUID("Owner", owner);
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.lifeTicks = tag.getInt("LifeTicks");
		if (tag.hasUUID("Owner")) {
			setOwnerUuid(tag.getUUID("Owner"));
		}
	}
}
