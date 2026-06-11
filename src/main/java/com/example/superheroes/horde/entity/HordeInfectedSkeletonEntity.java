package com.example.superheroes.horde.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class HordeInfectedSkeletonEntity extends BaseHordeEntity implements RangedAttackMob {
	public HordeInfectedSkeletonEntity(EntityType<? extends HordeInfectedSkeletonEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 22.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 2.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0, 20, 15.0f));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
		double dx = target.getX() - getX();
		double dy = target.getY(0.3333) - arrow.getY();
		double dz = target.getZ() - getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, 8.0f);
		arrow.setBaseDamage(arrow.getBaseDamage() + 1.5);
		level().addFreshEntity(arrow);
	}
}
