package com.example.superheroes.horde.entity;

import com.example.superheroes.horde.HordeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Base class for all horde entities. Tracks the horde instance they belong to
 * and notifies the HordeManager on death.
 */
public abstract class BaseHordeEntity extends Monster {
	private UUID hordeId;

	protected BaseHordeEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 5;
	}

	public static AttributeSupplier.Builder createBaseHordeAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
	}

	public void setHordeId(UUID hordeId) {
		this.hordeId = hordeId;
	}

	public UUID getHordeId() {
		return hordeId;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide() && hordeId != null) {
			HordeManager.onMobKilled(hordeId);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (hordeId != null) tag.putUUID("HordeId", hordeId);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("HordeId")) hordeId = tag.getUUID("HordeId");
	}

	@Override
	public boolean removeWhenFarAway(double distSq) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}
}
