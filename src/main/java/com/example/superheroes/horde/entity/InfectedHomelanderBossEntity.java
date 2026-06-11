package com.example.superheroes.horde.entity;

import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.horde.HordeManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Infected Homelander — final horde boss (Wave 10).
 * 800 HP, 3 phases:
 *  Phase 1 (>500 HP): Standard powerful attacks + eye lasers
 *  Phase 2 (500-200 HP): Parasitic Rage — faster, AoE poison, spawn crawlers
 *  Phase 3 (<200 HP): Berserk — massive damage, constant AoE, bleed on hit
 */
public class InfectedHomelanderBossEntity extends Monster {
	private final ServerBossEvent bossEvent = new ServerBossEvent(
			Component.literal("§4§l⚠ Заражённый Хоумлендер"),
			BossEvent.BossBarColor.RED,
			BossEvent.BossBarOverlay.NOTCHED_20);

	private UUID hordeId;
	private int phase = 1;
	private int slamCooldown = 0;
	private int screamCooldown = 0;
	private int laserCooldown = 0;
	private int spawnCooldown = 0;
	private int ticksAlive = 0;

	public InfectedHomelanderBossEntity(EntityType<? extends InfectedHomelanderBossEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 30, true);
		this.setNoGravity(true);
		this.xpReward = 200;
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
		nav.setCanOpenDoors(false);
		nav.setCanFloat(true);
		return nav;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 800.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FLYING_SPEED, 0.50)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.ARMOR, 20.0)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public void setHordeId(UUID id) {
		this.hordeId = id;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;

		ticksAlive++;
		if (slamCooldown > 0) slamCooldown--;
		if (screamCooldown > 0) screamCooldown--;
		if (laserCooldown > 0) laserCooldown--;
		if (spawnCooldown > 0) spawnCooldown--;

		bossEvent.setProgress(getHealth() / getMaxHealth());

		int newPhase = getHealth() > 500f ? 1 : (getHealth() > 200f ? 2 : 3);
		if (newPhase != phase) {
			onPhaseTransition(newPhase);
			phase = newPhase;
		}

		LivingEntity target = getTarget();
		if (target == null) return;

		switch (phase) {
			case 1 -> tickPhase1(target);
			case 2 -> tickPhase2(target);
			case 3 -> tickPhase3(target);
		}
	}

	private void onPhaseTransition(int newPhase) {
		if (!(level() instanceof ServerLevel sl)) return;
		if (newPhase == 2) {
			sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0, getZ(), 50, 3.0, 2.0, 3.0, 0.15);
			playSound(SoundEvents.WARDEN_ROAR, 2.0f, 0.6f);
			bossEvent.setName(Component.literal("§5§l⚠ Хоумлендер — ПАРАЗИТИЧЕСКАЯ ЯРОСТЬ"));
			broadcastMessage("§5Хоумлендер пронзительно кричит... паразит пробуждается!");
		} else if (newPhase == 3) {
			sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.0, getZ(), 3, 0, 0, 0, 0);
			sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.0, getZ(), 100, 5.0, 3.0, 5.0, 0.2);
			playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0f, 0.3f);
			playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.5f);
			bossEvent.setName(Component.literal("§4§l☠ Хоумлендер — БЕРСЕРК"));
			broadcastMessage("§4§l⚠ Хоумлендер полностью потерял контроль! БЕРСЕРК!");
			addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 2, false, true, false));
			addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 1, false, true, false));
		}
	}

	private void tickPhase1(LivingEntity target) {
		if (distanceTo(target) < 5.0 && slamCooldown <= 0) {
			slam(10.0f, 3.0);
			slamCooldown = 100;
		}
		if (laserCooldown <= 0) {
			eyeLaserAttack(target);
			laserCooldown = 80;
		}
	}

	private void tickPhase2(LivingEntity target) {
		if (distanceTo(target) < 6.0 && slamCooldown <= 0) {
			slam(14.0f, 4.0);
			slamCooldown = 80;
		}
		if (screamCooldown <= 0) {
			parasiticScream();
			screamCooldown = 150;
		}
		if (laserCooldown <= 0) {
			eyeLaserAttack(target);
			laserCooldown = 60;
		}
	}

	private void tickPhase3(LivingEntity target) {
		if (distanceTo(target) < 7.0 && slamCooldown <= 0) {
			slam(20.0f, 5.0);
			slamCooldown = 50;
		}
		if (screamCooldown <= 0) {
			parasiticScream();
			screamCooldown = 80;
		}
		if (laserCooldown <= 0) {
			eyeLaserAttack(target);
			laserCooldown = 40;
		}
		// Constant crimson aura
		if (ticksAlive % 20 == 0 && level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.0, getZ(), 20, 3.0, 1.5, 3.0, 0.05);
			AABB area = getBoundingBox().inflate(4.0);
			for (Player p : level().getEntitiesOfClass(Player.class, area)) {
				p.hurt(damageSources().mobAttack(this), 3.0f);
			}
		}
	}

	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit && phase >= 3 && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 100, 1, false, true, true));
		}
		return hit;
	}

	private void slam(float damage, double radius) {
		if (!(level() instanceof ServerLevel sl)) return;
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.5f);
		sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 2, 0, 0, 0, 0);
		sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.0, getZ(), 8, radius * 0.5, 0.3, radius * 0.5, 0.0);
		AABB area = getBoundingBox().inflate(radius);
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (e == this) continue;
			e.hurt(damageSources().mobAttack(this), damage);
			e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
			e.hurtMarked = true;
		}
	}

	private void eyeLaserAttack(LivingEntity target) {
		if (!(level() instanceof ServerLevel sl)) return;
		playSound(SoundEvents.BLAZE_SHOOT, 1.0f, 2.0f);
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 0.001) return;
		dx /= dist;
		dz /= dist;
		float laserDmg = phase >= 3 ? 12.0f : 8.0f;
		for (double d = 0; d < Math.min(dist, 20.0); d += 0.5) {
			double px = getX() + dx * d;
			double pz = getZ() + dz * d;
			sl.sendParticles(ParticleTypes.SMALL_FLAME, px, getEyeY(), pz, 1, 0.05, 0.05, 0.05, 0.0);
		}
		if (distanceTo(target) < 20.0) {
			target.hurt(damageSources().mobAttack(this), laserDmg);
			target.setRemainingFireTicks(60);
		}
	}

	private void parasiticScream() {
		if (!(level() instanceof ServerLevel sl)) return;
		playSound(SoundEvents.WARDEN_ROAR, 1.5f, 0.5f);
		sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
		AABB area = getBoundingBox().inflate(8.0);
		for (Player p : level().getEntitiesOfClass(Player.class, area)) {
			p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
			p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
			p.hurt(damageSources().mobAttack(this), 6.0f);
		}
	}

	private void broadcastMessage(String msg) {
		for (ServerPlayer p : bossEvent.getPlayers()) {
			p.sendSystemMessage(Component.literal(msg));
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		bossEvent.removeAllPlayers();
		if (!level().isClientSide() && hordeId != null) {
			HordeManager.onMobKilled(hordeId);
		}
		broadcastDeathMessage();
	}

	private void broadcastDeathMessage() {
		if (!(level() instanceof ServerLevel sl)) return;
		sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.0, getZ(), 5, 1.0, 1.0, 1.0, 0.0);
		for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
			p.sendSystemMessage(Component.literal("§6§l★ Заражённый Хоумлендер повержен! Орда побеждена! ★"));
		}
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
