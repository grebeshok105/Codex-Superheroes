package com.example.superheroes.entity;

import com.example.superheroes.entity.ai.HomelanderBlockThrowGoal;
import com.example.superheroes.entity.ai.HomelanderEyeLaserGoal;
import com.example.superheroes.entity.ai.HomelanderFlightGoal;
import com.example.superheroes.entity.ai.HomelanderGroundMagnetGoal;
import com.example.superheroes.entity.ai.HomelanderHandClapGoal;
import com.example.superheroes.entity.ai.HomelanderHeatVisionSweepGoal;
import com.example.superheroes.entity.ai.HomelanderLightningCallGoal;
import com.example.superheroes.entity.ai.HomelanderRoarGoal;
import com.example.superheroes.entity.ai.HomelanderShockwaveDiveGoal;
import com.example.superheroes.entity.ai.HomelanderSonicSlamGoal;
import com.example.superheroes.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class HomelanderBossEntity extends Monster {
	private final ServerBossEvent bossEvent = new ServerBossEvent(
			Component.translatable("entity.superheroes.homelander_boss"),
			BossEvent.BossBarColor.GREEN,
			BossEvent.BossBarOverlay.NOTCHED_10);

	private int laserCooldown;
	private int shockwaveCooldown;
	private int sweepCooldown;
	private int lightningCooldown;
	private int slamCooldown;
	private int magnetCooldown;
	private int throwCooldown;
	private int roarCooldown;
	private int handClapCooldown;

	public HomelanderBossEntity(EntityType<? extends HomelanderBossEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 30, true);
		this.setNoGravity(true);
		this.xpReward = 50;
		this.shockwaveCooldown = 200;
		this.sweepCooldown = 240;
		this.lightningCooldown = 100;
		this.slamCooldown = 160;
		this.magnetCooldown = 280;
		this.throwCooldown = 200;
		this.roarCooldown = 220;
		this.handClapCooldown = 180;
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
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 500.0)
				.add(Attributes.ARMOR, 100.0)
				.add(Attributes.ARMOR_TOUGHNESS, 12.0)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.ATTACK_KNOCKBACK, 2.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.8)
				.add(Attributes.FLYING_SPEED, 1.4)
				.add(Attributes.FOLLOW_RANGE, 96.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new HomelanderRoarGoal(this));
		this.goalSelector.addGoal(1, new HomelanderGroundMagnetGoal(this));
		this.goalSelector.addGoal(1, new HomelanderShockwaveDiveGoal(this));
		this.goalSelector.addGoal(1, new HomelanderHeatVisionSweepGoal(this));
		this.goalSelector.addGoal(1, new HomelanderSonicSlamGoal(this));
		this.goalSelector.addGoal(1, new HomelanderHandClapGoal(this));
		this.goalSelector.addGoal(2, new HomelanderEyeLaserGoal(this));
		this.goalSelector.addGoal(2, new HomelanderLightningCallGoal(this));
		this.goalSelector.addGoal(2, new HomelanderBlockThrowGoal(this));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.4, true));
		this.goalSelector.addGoal(4, new HomelanderFlightGoal(this));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24f));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (laserCooldown > 0) laserCooldown--;
		if (shockwaveCooldown > 0) shockwaveCooldown--;
		if (sweepCooldown > 0) sweepCooldown--;
		if (lightningCooldown > 0) lightningCooldown--;
		if (slamCooldown > 0) slamCooldown--;
		if (magnetCooldown > 0) magnetCooldown--;
		if (throwCooldown > 0) throwCooldown--;
		if (roarCooldown > 0) roarCooldown--;
		if (handClapCooldown > 0) handClapCooldown--;
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
		if (!(this.level() instanceof ServerLevel sl)) {
			return super.doHurtTarget(target);
		}
		float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		DamageSource ds = com.example.superheroes.damage.ModDamageTypes.homelanderMelee(sl, this);
		boolean hurt = target.hurt(ds, damage);
		if (hurt) {
			if (target instanceof LivingEntity le) {
				le.knockback(0.5F,
						Math.sin(this.getYRot() * (float) Math.PI / 180F),
						-Math.cos(this.getYRot() * (float) Math.PI / 180F));
				this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
			}
			this.setLastHurtMob(target);
		}
		return hurt;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean wasRecentlyHit) {
		super.dropCustomDeathLoot(level, damageSource, wasRecentlyHit);
		this.spawnAtLocation(new ItemStack(ModItems.HOMELANDER_SUIT));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDER_DRAGON_GROWL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
		return super.canBeAffected(effect);
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	public int getLaserCooldown() {
		return laserCooldown;
	}

	public void setLaserCooldown(int ticks) {
		this.laserCooldown = ticks;
	}

	public int getShockwaveCooldown() {
		return shockwaveCooldown;
	}

	public void setShockwaveCooldown(int ticks) {
		this.shockwaveCooldown = ticks;
	}

	public int getSweepCooldown() {
		return sweepCooldown;
	}

	public void setSweepCooldown(int ticks) {
		this.sweepCooldown = ticks;
	}

	public int getLightningCooldown() {
		return lightningCooldown;
	}

	public void setLightningCooldown(int ticks) {
		this.lightningCooldown = ticks;
	}

	public int getSlamCooldown() {
		return slamCooldown;
	}

	public void setSlamCooldown(int ticks) {
		this.slamCooldown = ticks;
	}

	public int getMagnetCooldown() {
		return magnetCooldown;
	}

	public void setMagnetCooldown(int ticks) {
		this.magnetCooldown = ticks;
	}

	public int getThrowCooldown() {
		return throwCooldown;
	}

	public void setThrowCooldown(int ticks) {
		this.throwCooldown = ticks;
	}

	public int getRoarCooldown() {
		return roarCooldown;
	}

	public void setRoarCooldown(int ticks) {
		this.roarCooldown = ticks;
	}

	public int getHandClapCooldown() {
		return handClapCooldown;
	}

	public void setHandClapCooldown(int ticks) {
		this.handClapCooldown = ticks;
	}
}
