package com.example.superheroes.entity;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Теневой солдат Сон Джи Ву.
 *
 *  - Расширяет PathfinderMob (НЕ Monster), чтобы голем/волк/etc по умолчанию
 *    не агрились на него. После того как сам солдат ударит моба — у этого
 *    моба сработает HurtByTargetGoal и он начнёт бить тень в ответ.
 *  - Полёт + наземные варианты (флаг grounded).
 *  - Формация полукруга вокруг хозяина: каждый солдат хранит slotIndex/slotCount.
 *  - Атакует: focus → последний хёрт-моб владельца → последний агрессор владельца.
 *  - Урон от хозяина игнорирует.
 */
public class ShadowSoldierEntity extends PathfinderMob {
	public static final int VARIANT_COUNT = 3;
	public static final double FOLLOW_RADIUS = 6.0;

	private static final EntityDataAccessor<Byte> DATA_VARIANT =
			SynchedEntityData.defineId(ShadowSoldierEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_GROUNDED =
			SynchedEntityData.defineId(ShadowSoldierEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
			SynchedEntityData.defineId(ShadowSoldierEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Optional<UUID>> DATA_FOCUS =
			SynchedEntityData.defineId(ShadowSoldierEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private int slotIndex = 0;
	private int slotCount = 1;

	public ShadowSoldierEntity(EntityType<? extends ShadowSoldierEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 30, true);
		this.setNoGravity(true);
		this.xpReward = 0;
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
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ARMOR, 5.0)
				.add(Attributes.MOVEMENT_SPEED, 0.45)
				.add(Attributes.FLYING_SPEED, 0.65)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, (byte) 0);
		builder.define(DATA_GROUNDED, (byte) 0);
		builder.define(DATA_OWNER, Optional.empty());
		builder.define(DATA_FOCUS, Optional.empty());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		// Тень мстит тому, кто её ударил — но targetSelector не должен
		// сам сканировать чужих мобов. HurtByTargetGoal реагирует ТОЛЬКО на
		// агрессора (после первого удара по тени), так что моба «по умолчанию»
		// не атакуем — только в ответ.
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	public int getVariant() {
		return this.entityData.get(DATA_VARIANT) & 0xFF;
	}

	public void setVariant(int variant) {
		this.entityData.set(DATA_VARIANT, (byte) (variant % VARIANT_COUNT));
	}

	public boolean isGrounded() {
		return this.entityData.get(DATA_GROUNDED) != 0;
	}

	public void setGrounded(boolean grounded) {
		this.entityData.set(DATA_GROUNDED, (byte) (grounded ? 1 : 0));
		this.setNoGravity(!grounded);
	}

	public void setSlot(int index, int count) {
		this.slotIndex = index;
		this.slotCount = Math.max(1, count);
	}

	@Nullable
	public UUID getOwnerId() {
		return this.entityData.get(DATA_OWNER).orElse(null);
	}

	public void setOwnerId(@Nullable UUID id) {
		this.entityData.set(DATA_OWNER, Optional.ofNullable(id));
	}

	@Nullable
	public UUID getFocusId() {
		return this.entityData.get(DATA_FOCUS).orElse(null);
	}

	public void setFocusId(@Nullable UUID id) {
		this.entityData.set(DATA_FOCUS, Optional.ofNullable(id));
	}

	@Nullable
	public Player getOwner() {
		UUID id = getOwnerId();
		if (id == null) return null;
		Player p = this.level().getPlayerByUUID(id);
		return p != null && p.isAlive() ? p : null;
	}

	@Nullable
	public LivingEntity getFocus() {
		UUID id = getFocusId();
		if (id == null) return null;
		if (!(this.level() instanceof ServerLevel sl)) return null;
		Entity e = sl.getEntity(id);
		if (e instanceof LivingEntity le && le.isAlive()) return le;
		return null;
	}

	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnReason) {
		return false;
	}

	@Override
	public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENDERMAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENDERMAN_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.18f;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!this.level().isClientSide() && this.tickCount % 10 == 0) {
			updatePursuitSpeed();
		}
		if (this.level().isClientSide()) {
			if (this.tickCount % 4 == 0) {
				double x = this.getX() + (this.random.nextDouble() - 0.5) * 0.3;
				double y = this.getY() + this.random.nextDouble() * 0.5;
				double z = this.getZ() + (this.random.nextDouble() - 0.5) * 0.3;
				this.level().addParticle(ParticleTypes.WARPED_SPORE, x, y, z, 0.0, 0.02, 0.0);
			}
			return;
		}

		Player owner = getOwner();
		LivingEntity desired = chooseTarget(owner);
		// Подхватываем активный агрессор-таргет (HurtByTargetGoal) — не сбрасываем его.
		LivingEntity current = this.getTarget();
		boolean keepingHurtTarget = current != null && current.isAlive()
				&& current == this.getLastHurtByMob();
		if (!keepingHurtTarget && desired != current) {
			this.setTarget(desired);
		}

		// Если цели нет — занять свою позицию в полукруге за хозяином
		if (this.getTarget() == null && owner != null) {
			Vec3 slotPos = computeSlotPosition(owner);
			double d2 = this.position().distanceToSqr(slotPos);
			if (d2 > FOLLOW_RADIUS * FOLLOW_RADIUS) {
				if (isGrounded()) {
					this.getNavigation().moveTo(slotPos.x, slotPos.y, slotPos.z, 1.5);
				} else {
					this.getMoveControl().setWantedPosition(slotPos.x, slotPos.y, slotPos.z, 1.5);
				}
			}
		}
	}

	@Nullable
	private LivingEntity chooseTarget(@Nullable Player owner) {
		LivingEntity focus = getFocus();
		if (focus != null && focus.isAlive() && !isOwner(focus)) return focus;
		if (owner == null) return null;
		// Кого хозяин последним ударил
		LivingEntity attackedByOwner = owner.getLastHurtMob();
		if (attackedByOwner != null && attackedByOwner.isAlive() && !isOwner(attackedByOwner)
				&& !(attackedByOwner instanceof ShadowSoldierEntity)) {
			return attackedByOwner;
		}
		// Кто последний ударил хозяина
		LivingEntity ownersAttacker = owner.getLastHurtByMob();
		if (ownersAttacker != null && ownersAttacker.isAlive() && !isOwner(ownersAttacker)
				&& !(ownersAttacker instanceof ShadowSoldierEntity)) {
			return ownersAttacker;
		}
		return null;
	}

	private static final net.minecraft.resources.ResourceLocation PURSUIT_MOD =
			com.example.superheroes.ModId.of("shadow_soldier/pursuit_speed");

	private void updatePursuitSpeed() {
		net.minecraft.world.entity.ai.attributes.AttributeInstance moveAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
		net.minecraft.world.entity.ai.attributes.AttributeInstance flyAttr = this.getAttribute(Attributes.FLYING_SPEED);
		if (moveAttr != null) moveAttr.removeModifier(PURSUIT_MOD);
		if (flyAttr != null) flyAttr.removeModifier(PURSUIT_MOD);
		LivingEntity tgt = this.getTarget();
		if (tgt == null || !tgt.isAlive()) return;
		Vec3 v = tgt.getDeltaMovement();
		double speed = Math.sqrt(v.x * v.x + v.z * v.z);
		double extra = Math.max(0.30, speed * 6.0);
		if (extra > 5.0) extra = 5.0;
		net.minecraft.world.entity.ai.attributes.AttributeModifier mod = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
				PURSUIT_MOD, extra, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		if (moveAttr != null) moveAttr.addTransientModifier(mod);
		if (flyAttr != null) flyAttr.addTransientModifier(mod);
	}

	private boolean isOwner(LivingEntity e) {
		UUID id = getOwnerId();
		return id != null && id.equals(e.getUUID());
	}

	private Vec3 computeSlotPosition(Player owner) {
		double radius = isGrounded() ? 3.0 : 4.0;
		// Полукруг сзади хозяина: yaw + 180 ± 90°
		double baseYawRad = Math.toRadians(owner.getYRot() + 180.0);
		double slotAngle = (slotCount > 1)
				? (slotIndex - (slotCount - 1) / 2.0) * (Math.PI / Math.max(1, slotCount - 1)) * 0.95
				: 0.0;
		double angle = baseYawRad + slotAngle;
		double dx = -Math.sin(angle) * radius;
		double dz = Math.cos(angle) * radius;
		double targetX = owner.getX() + dx;
		double targetZ = owner.getZ() + dz;
		double targetY;
		if (isGrounded()) {
			BlockPos here = BlockPos.containing(targetX, owner.getY() + 1.0, targetZ);
			targetY = owner.getY();
			// Найдём ближайшую стоячую поверхность
			for (int i = 0; i < 4; i++) {
				BlockPos below = here.below(i);
				if (this.level().getBlockState(below).isSolid()) {
					targetY = below.getY() + 1.0;
					break;
				}
			}
		} else {
			targetY = owner.getY() + 2.5 + ((slotIndex % 3) * 0.4);
		}
		return new Vec3(targetX, targetY, targetZ);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity ent = source.getEntity();
		if (ent != null && ent.getUUID().equals(getOwnerId())) {
			return false;
		}
		// Тени не бьют сами себя
		if (ent instanceof ShadowSoldierEntity other && hasSameOwner(other)) {
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		if (!(this.level() instanceof ServerLevel sl)) {
			return super.doHurtTarget(target);
		}
		float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		boolean dealt = target.hurt(ModDamageTypes.shadowAttack(sl, this), damage);
		if (dealt) {
			this.setLastHurtMob(target);
		}
		return dealt;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (target.getUUID().equals(getOwnerId())) return false;
		if (target instanceof ShadowSoldierEntity other && hasSameOwner(other)) return false;
		return super.canAttack(target);
	}

	private boolean hasSameOwner(ShadowSoldierEntity other) {
		UUID a = this.getOwnerId();
		UUID b = other.getOwnerId();
		return a != null && a.equals(b);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("Variant", (byte) getVariant());
		tag.putByte("Grounded", (byte) (isGrounded() ? 1 : 0));
		tag.putInt("SlotIndex", slotIndex);
		tag.putInt("SlotCount", slotCount);
		UUID owner = getOwnerId();
		if (owner != null) tag.putUUID("Owner", owner);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setVariant(tag.getByte("Variant"));
		setGrounded(tag.getByte("Grounded") != 0);
		this.slotIndex = tag.getInt("SlotIndex");
		this.slotCount = Math.max(1, tag.getInt("SlotCount"));
		if (tag.hasUUID("Owner")) {
			setOwnerId(tag.getUUID("Owner"));
		}
	}
}
