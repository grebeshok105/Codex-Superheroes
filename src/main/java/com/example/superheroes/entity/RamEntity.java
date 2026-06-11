package com.example.superheroes.entity;

import com.example.superheroes.effect.RamCompanionController;
import com.example.superheroes.effect.RemDemonismController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Рам — призванная помощница Рем в форме демонизма.
 *
 *  - Защищает хозяйку: целится в того, кто атакует Рем или кого атакует Рем.
 *  - Гибрид: вблизи дерётся в ближнем бою и бьёт морозной волной, издалека кастует ледяные снаряды.
 *  - Держится рядом с Рем; если застряла (яма, обрыв) или отошла слишком далеко — телепортируется к хозяйке.
 *  - Мобы агрятся на неё как на игрока-защитника.
 *  - При <30% HP отступает к хозяйке и лечится со временем.
 *  - Живёт только пока активен демонизм; её смерть ослабляет Рем (см. RamCompanionController).
 */
public class RamEntity extends PathfinderMob {
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
			SynchedEntityData.defineId(RamEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private static final double TELEPORT_DISTANCE = 20.0;
	private static final double TARGET_SEARCH_AROUND_OWNER = 14.0;
	private static final int STUCK_CHECK_INTERVAL = 40;
	private static final double STUCK_MOVE_THRESHOLD_SQR = 0.5 * 0.5;
	private static final int FROST_NOVA_COOLDOWN = 8 * 20;
	private static final double FROST_NOVA_RADIUS = 3.2;
	private static final float FROST_NOVA_DAMAGE = 5.0f;

	private Vec3 lastStuckCheckPos = Vec3.ZERO;
	private int stuckStrikes;
	private int frostNovaCooldown;
	private CombatStyle combatStyle = CombatStyle.RANGED;
	private int styleSwapTicks;
	private boolean provokeAlternator;
	/** Timestamp of the owner's last melee hit we already reacted to (priority order). */
	private int ownerOrderStamp = Integer.MIN_VALUE;

	/** Режим боя: автоматически переключается между ближним и дальним. */
	enum CombatStyle {
		MELEE, RANGED
	}

	public RamEntity(EntityType<? extends RamEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.33)
				.add(Attributes.ATTACK_DAMAGE, 4.5)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER, Optional.empty());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new RetreatToOwnerGoal(this));
		this.goalSelector.addGoal(2, new RamMeleeGoal(this));
		this.goalSelector.addGoal(3, new IceBoltAttackGoal(this));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Nullable
	public UUID getOwnerId() {
		return this.entityData.get(DATA_OWNER).orElse(null);
	}

	public void setOwnerId(@Nullable UUID id) {
		this.entityData.set(DATA_OWNER, Optional.ofNullable(id));
	}

	@Nullable
	public Player getOwner() {
		UUID id = getOwnerId();
		if (id == null) return null;
		Player p = this.level().getPlayerByUUID(id);
		return p != null && p.isAlive() ? p : null;
	}

	private boolean isOwner(LivingEntity entity) {
		UUID id = getOwnerId();
		return id != null && id.equals(entity.getUUID());
	}

	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnReason) {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.getEntity() instanceof LivingEntity attacker && isOwner(attacker)) {
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide()) {
			if (this.tickCount % 5 == 0) {
				this.level().addParticle(ParticleTypes.CHERRY_LEAVES,
						this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
						this.getY() + 1.5 + this.random.nextDouble() * 0.3,
						this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
						0.0, 0.02, 0.0);
			}
			return;
		}
		if (frostNovaCooldown > 0) {
			frostNovaCooldown--;
		}
		Player owner = getOwner();
		if (this.tickCount % 20 == 0) {
			if (owner == null || !RemDemonismController.isActive(owner)) {
				dismiss();
				return;
			}
			if (this.getTarget() == null && this.getHealth() < this.getMaxHealth()) {
				this.heal(1.0f);
			}
			provokeNearbyMonsters(owner);
		}
		if (owner != null) {
			keepNearOwner(owner);
		}
		if (this.tickCount % 10 == 0) {
			updateTargeting(owner);
			updateCombatStyle();
			updateHeldWand();
		}
		tryFrostNova();
	}

	/**
	 * Жёсткая логика цели (по приказу хозяйки):
	 * 1. Кого хозяйка ударила последним — это новый приказ, Рам переключается на него.
	 * 2. Пока приказ/цель жив — Рам добивает его БЕЗ смены приоритетов,
	 *    даже если хозяйка ушла далеко.
	 * 3. Только когда цели нет — защитный режим: те, кто бьёт её или хозяйку,
	 *    иначе монстры рядом с хозяйкой.
	 */
	private void updateTargeting(@Nullable Player owner) {
		LivingEntity current = this.getTarget();
		if (owner != null) {
			LivingEntity ordered = owner.getLastHurtMob();
			int stamp = owner.getLastHurtMobTimestamp();
			if (ordered != null && stamp != ownerOrderStamp && isValidTarget(ordered)) {
				ownerOrderStamp = stamp;
				if (ordered != current) {
					this.setTarget(ordered);
				}
				return;
			}
		}
		if (current != null && isValidTarget(current)) {
			return; // цель жива — никаких переключений
		}
		LivingEntity fallback = chooseTarget(owner);
		if (fallback != current) {
			this.setTarget(fallback);
		}
	}

	/** В дальнем режиме Рам держит «жезл» — зачарованную палочку; в ближнем руки свободны. */
	private void updateHeldWand() {
		boolean wantWand = this.getTarget() != null && combatStyle == CombatStyle.RANGED;
		boolean hasWand = !this.getMainHandItem().isEmpty();
		if (wantWand && !hasWand) {
			ItemStack wand = new ItemStack(Items.STICK);
			wand.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			this.setItemSlot(EquipmentSlot.MAINHAND, wand);
			this.setDropChance(EquipmentSlot.MAINHAND, 0f);
		} else if (!wantWand && hasWand) {
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
	}

	/**
	 * Держит Рам рядом с хозяйкой ТОЛЬКО вне боя: пока есть живая цель — никаких
	 * телепортов к хозяйке и сбросов цели (Рам обязана добить приказ). При
	 * застревании в бою телепортируется к цели, вне боя — к хозяйке.
	 */
	private void keepNearOwner(Player owner) {
		LivingEntity target = this.getTarget();
		boolean fighting = target != null && target.isAlive();
		double distSqr = this.distanceToSqr(owner);
		if (!fighting && distSqr > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
			teleportNear(owner);
			return;
		}
		if (this.tickCount % STUCK_CHECK_INTERVAL == 0) {
			boolean wantsToMove = this.getNavigation().isInProgress()
					|| (!fighting && distSqr > 7.0 * 7.0);
			boolean barelyMoved = this.position().distanceToSqr(lastStuckCheckPos) < STUCK_MOVE_THRESHOLD_SQR;
			if (wantsToMove && barelyMoved) {
				stuckStrikes++;
				if (stuckStrikes >= 2) {
					teleportNear(fighting ? target : owner);
				}
			} else {
				stuckStrikes = 0;
			}
			lastStuckCheckPos = this.position();
		}
	}

	private void teleportNear(LivingEntity anchor) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		for (int attempt = 0; attempt < 12; attempt++) {
			double dx = anchor.getX() + (this.random.nextDouble() - 0.5) * 4.0;
			double dz = anchor.getZ() + (this.random.nextDouble() - 0.5) * 4.0;
			double dy = anchor.getY() + this.random.nextInt(2);
			BlockPos pos = BlockPos.containing(dx, dy, dz);
			AABB box = this.getDimensions(this.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(pos));
			if (level.noCollision(this, box) && !level.getBlockState(pos.below()).isAir()) {
				level.sendParticles(ParticleTypes.POOF,
						this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
				this.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				this.getNavigation().stop();
				this.stuckStrikes = 0;
				this.lastStuckCheckPos = this.position();
				level.sendParticles(ParticleTypes.CHERRY_LEAVES,
						this.getX(), this.getY() + 1.0, this.getZ(), 16, 0.4, 0.7, 0.4, 0.04);
				level.playSound(null, this.getX(), this.getY(), this.getZ(),
						SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.6f, 1.7f);
				return;
			}
		}
	}

	/**
	 * Мобы должны видеть в Рам угрозу: враждебные мобы без цели агрятся на неё,
	 * а мобы, дерущиеся с хозяйкой (включая нейтральных вроде големов), делят
	 * агро между Рем и Рам по очереди.
	 */
	private void provokeNearbyMonsters(Player owner) {
		AABB box = this.getBoundingBox().inflate(12.0, 6.0, 12.0);
		List<Mob> mobs = this.level().getEntitiesOfClass(Mob.class, box,
				mob -> mob.isAlive() && !mob.isNoAi() && mob != this
						&& (mob instanceof Enemy || mob.getTarget() == owner));
		for (Mob mob : mobs) {
			LivingEntity mobTarget = mob.getTarget();
			if (mobTarget == null) {
				aggro(mob);
			} else if (mobTarget == owner) {
				provokeAlternator = !provokeAlternator;
				if (provokeAlternator) {
					aggro(mob);
				}
			}
		}
	}

	/** Переагрить моба на Рам; нейтральным (голем, пчёлы и т.п.) ещё и ставится злость. */
	private void aggro(Mob mob) {
		mob.setTarget(this);
		mob.setLastHurtByMob(this);
		if (mob instanceof NeutralMob neutral) {
			neutral.setPersistentAngerTarget(this.getUUID());
			neutral.startPersistentAngerTimer();
		}
	}

	/** Цель, которую Рам ударила, должна ответить ей, а не хозяйке. */
	private void tauntOnHit(LivingEntity target) {
		if (target instanceof Mob mob && mob.getTarget() != this) {
			aggro(mob);
		}
	}

	/** Автопереключение режима боя: летающий враг — только дальний, на земле — по ситуации. */
	private void updateCombatStyle() {
		LivingEntity target = this.getTarget();
		if (target == null) {
			return;
		}
		if (styleSwapTicks > 0) {
			styleSwapTicks -= 10;
		}
		if (isAirborne(target)) {
			combatStyle = CombatStyle.RANGED;
			return;
		}
		double distSqr = this.distanceToSqr(target);
		if (distSqr > 9.0 * 9.0) {
			combatStyle = CombatStyle.RANGED;
		} else if (distSqr < 3.5 * 3.5) {
			combatStyle = CombatStyle.MELEE;
		} else if (styleSwapTicks <= 0) {
			combatStyle = combatStyle == CombatStyle.MELEE ? CombatStyle.RANGED : CombatStyle.MELEE;
			styleSwapTicks = 120;
		}
	}

	/** Летит ли цель: не на земле и под ней минимум 3 блока воздуха. */
	private boolean isAirborne(LivingEntity target) {
		if (target.onGround()) {
			return false;
		}
		if (target.isFallFlying()) {
			return true;
		}
		BlockPos.MutableBlockPos pos = target.blockPosition().mutable();
		for (int i = 1; i <= 3; i++) {
			pos.move(0, -1, 0);
			if (!this.level().getBlockState(pos).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Морозная волна при окружении: урон, замедление и откидывание врагов вплотную. */
	private void tryFrostNova() {
		if (frostNovaCooldown > 0 || !(this.level() instanceof ServerLevel level)) {
			return;
		}
		AABB box = this.getBoundingBox().inflate(FROST_NOVA_RADIUS, 1.5, FROST_NOVA_RADIUS);
		List<LivingEntity> threats = level.getEntitiesOfClass(LivingEntity.class, box, this::isThreat);
		if (threats.size() < 2) {
			return;
		}
		frostNovaCooldown = FROST_NOVA_COOLDOWN;
		for (LivingEntity threat : threats) {
			threat.hurt(level.damageSources().mobAttack(this), FROST_NOVA_DAMAGE);
			threat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, true, true, true));
			Vec3 push = threat.position().subtract(this.position());
			double horiz = Math.max(0.01, Math.sqrt(push.x * push.x + push.z * push.z));
			threat.setDeltaMovement(push.x / horiz * 0.7, 0.25, push.z / horiz * 0.7);
			threat.hurtMarked = true;
		}
		level.sendParticles(ParticleTypes.SNOWFLAKE,
				this.getX(), this.getY() + 0.8, this.getZ(), 40, 1.4, 0.6, 1.4, 0.08);
		level.sendParticles(ParticleTypes.CLOUD,
				this.getX(), this.getY() + 0.3, this.getZ(), 14, 1.0, 0.2, 1.0, 0.04);
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0f, 1.5f);
	}

	@Nullable
	private LivingEntity chooseTarget(@Nullable Player owner) {
		if (owner == null) return null;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isValidTarget(ownerAttacker)) return ownerAttacker;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isValidTarget(ownerTarget)) return ownerTarget;
		AABB box = owner.getBoundingBox().inflate(TARGET_SEARCH_AROUND_OWNER, 6.0, TARGET_SEARCH_AROUND_OWNER);
		List<Mob> mobs = this.level().getEntitiesOfClass(Mob.class, box,
				mob -> mob.isAlive() && mob != this
						&& (mob.getTarget() == owner || (mob instanceof Monster && mob.getTarget() == null)));
		Mob closest = null;
		double best = Double.MAX_VALUE;
		for (Mob mob : mobs) {
			double d = mob.distanceToSqr(this);
			if (d < best) {
				best = d;
				closest = mob;
			}
		}
		return closest;
	}

	private boolean isValidTarget(@Nullable LivingEntity target) {
		if (target == null || !target.isAlive() || isOwner(target) || target instanceof RamEntity) {
			return false;
		}
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return false;
		}
		return target.level() == this.level();
	}

	/** Угроза для морозной волны: текущая цель, враждебные мобы и те, кто целится в Рам/хозяйку. */
	private boolean isThreat(LivingEntity entity) {
		if (!isValidTarget(entity)) {
			return false;
		}
		if (entity == this.getTarget() || entity instanceof Enemy) {
			return true;
		}
		Player owner = getOwner();
		return entity instanceof Mob mob && (mob.getTarget() == this || (owner != null && mob.getTarget() == owner));
	}

	public void dismiss() {
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CHERRY_LEAVES,
					this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.6, 0.4, 0.04);
			level.sendParticles(ParticleTypes.POOF,
					this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
			level.playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.7f, 1.6f);
		}
		this.discard();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!this.level().isClientSide()) {
			RamCompanionController.onRamDeath(this);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		UUID owner = getOwnerId();
		if (owner != null) {
			tag.putUUID("RamOwner", owner);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("RamOwner")) {
			setOwnerId(tag.getUUID("RamOwner"));
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ALLAY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ALLAY_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.35f;
	}

	/** Ближний бой, когда цель рядом: обычные удары с лёгким морозным слоу. */
	private static final class RamMeleeGoal extends MeleeAttackGoal {
		private static final double ENGAGE_RANGE = 4.5;
		private static final double DISENGAGE_RANGE = 6.5;
		private final RamEntity ram;

		RamMeleeGoal(RamEntity ram) {
			super(ram, 1.25, true);
			this.ram = ram;
		}

		@Override
		public boolean canUse() {
			LivingEntity target = ram.getTarget();
			return ram.combatStyle == CombatStyle.MELEE && target != null
					&& ram.distanceToSqr(target) <= ENGAGE_RANGE * ENGAGE_RANGE && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = ram.getTarget();
			return ram.combatStyle == CombatStyle.MELEE && target != null
					&& ram.distanceToSqr(target) <= DISENGAGE_RANGE * DISENGAGE_RANGE
					&& super.canContinueToUse();
		}

		@Override
		protected void checkAndPerformAttack(LivingEntity target) {
			if (this.canPerformAttack(target)) {
				this.resetAttackCooldown();
				this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
				if (this.mob.doHurtTarget(target)) {
					ram.tauntOnHit(target);
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, true, true));
					if (ram.level() instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.SNOWFLAKE,
								target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
								8, 0.25, 0.25, 0.25, 0.04);
					}
				}
			}
		}
	}

	/** Дальний бой: держит дистанцию, стрейфится, кастует ледяные снаряды. */
	private static final class IceBoltAttackGoal extends Goal {
		private static final double MIN_RANGE = 5.0;
		private static final double MAX_RANGE = 11.0;
		private static final int CAST_INTERVAL = 35;
		private static final float BOLT_DAMAGE = 6.0f;
		private final RamEntity ram;
		private int castCooldown;
		private boolean strafeRight;

		IceBoltAttackGoal(RamEntity ram) {
			this.ram = ram;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = ram.getTarget();
			return ram.combatStyle == CombatStyle.RANGED && target != null && target.isAlive()
					&& ram.getHealth() > ram.getMaxHealth() * 0.3f;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = ram.getTarget();
			if (target == null) return;
			ram.getLookControl().setLookAt(target, 30.0f, 30.0f);
			double distSqr = ram.distanceToSqr(target);
			if (distSqr < MIN_RANGE * MIN_RANGE) {
				Vec3 away = ram.position().subtract(target.position()).normalize();
				Vec3 dest = ram.position().add(away.scale(5.0));
				ram.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.25);
			} else if (distSqr > MAX_RANGE * MAX_RANGE) {
				ram.getNavigation().moveTo(target, 1.1);
			} else {
				ram.getNavigation().stop();
				if (ram.tickCount % 30 == 0) {
					strafeRight = !strafeRight;
				}
				ram.getMoveControl().strafe(0.0f, strafeRight ? 0.5f : -0.5f);
			}
			if (castCooldown > 0) {
				castCooldown--;
				return;
			}
			if (distSqr <= MAX_RANGE * MAX_RANGE * 1.3 && ram.getSensing().hasLineOfSight(target)) {
				castBolt(target);
				castCooldown = CAST_INTERVAL;
			}
		}

		private void castBolt(LivingEntity target) {
			if (!(ram.level() instanceof ServerLevel level)) return;
			Vec3 from = ram.getEyePosition();
			Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 step = to.subtract(from).normalize().scale(0.6);
			Vec3 cursor = from.add(step);
			int steps = (int) Math.ceil(to.subtract(from).length() / 0.6);
			for (int i = 0; i < steps && i < 30; i++) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, cursor.x, cursor.y, cursor.z, 2, 0.05, 0.05, 0.05, 0.0);
				level.sendParticles(ParticleTypes.END_ROD, cursor.x, cursor.y, cursor.z, 1, 0.02, 0.02, 0.02, 0.0);
				cursor = cursor.add(step);
			}
			ram.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			target.hurt(level.damageSources().mobAttack(ram), BOLT_DAMAGE);
			ram.tauntOnHit(target);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, true, true, true));
			level.playSound(null, ram.getX(), ram.getY(), ram.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8f, 1.7f);
		}
	}

	/** Отступление к хозяйке при низком HP. */
	private static final class RetreatToOwnerGoal extends Goal {
		private final RamEntity ram;

		RetreatToOwnerGoal(RamEntity ram) {
			this.ram = ram;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return ram.getHealth() <= ram.getMaxHealth() * 0.3f && ram.getOwner() != null;
		}

		@Override
		public void tick() {
			Player owner = ram.getOwner();
			if (owner == null) return;
			if (ram.distanceToSqr(owner) > 9.0) {
				ram.getNavigation().moveTo(owner, 1.35);
			} else {
				ram.getNavigation().stop();
			}
			if (ram.tickCount % 40 == 0) {
				ram.heal(2.0f);
				if (ram.level() instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.HEART,
							ram.getX(), ram.getY() + 1.6, ram.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
				}
			}
		}
	}

	/** Следование за хозяйкой, когда нет цели: держится в паре блоков. */
	private static final class FollowOwnerGoal extends Goal {
		private final RamEntity ram;

		FollowOwnerGoal(RamEntity ram) {
			this.ram = ram;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			Player owner = ram.getOwner();
			return ram.getTarget() == null && owner != null && ram.distanceToSqr(owner) > 6.0 * 6.0;
		}

		@Override
		public boolean canContinueToUse() {
			Player owner = ram.getOwner();
			return ram.getTarget() == null && owner != null && ram.distanceToSqr(owner) > 3.0 * 3.0;
		}

		@Override
		public void tick() {
			Player owner = ram.getOwner();
			if (owner != null) {
				ram.getNavigation().moveTo(owner, 1.2);
			}
		}
	}
}
