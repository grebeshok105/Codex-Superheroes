package com.example.superheroes.horde.entity;

import com.example.superheroes.horde.entity.projectile.HordeAcidBombEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Infector — ranged poison support. Lobs an acid bomb (lingering poison cloud)
 * from afar, and still injects Poison + Weakness on melee contact when cornered.
 */
public class HordeInfectorEntity extends BaseHordeEntity implements RangedAttackMob {
	public HordeInfectorEntity(EntityType<? extends HordeInfectorEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHordeAttributes()
				.add(Attributes.MAX_HEALTH, 28.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.ARMOR, 2.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.95, 50, 18.0f));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		HordeAcidBombEntity bomb = new HordeAcidBombEntity(this, level());
		double dx = target.getX() - getX();
		double dy = target.getY(0.5) - bomb.getY();
		double dz = target.getZ() - getZ();
		double horiz = Math.sqrt(dx * dx + dz * dz);
		bomb.shoot(dx, dy + horiz * 0.20, dz, 0.95f, 5.0f);
		level().addFreshEntity(bomb);
		playSound(SoundEvents.SLIME_SQUISH, 1.0f, 0.8f);
		if (level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 6, 0.2, 0.2, 0.2, 0.03);
		}
	}

	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, true, true));
			living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
		}
		return hit;
	}
}
