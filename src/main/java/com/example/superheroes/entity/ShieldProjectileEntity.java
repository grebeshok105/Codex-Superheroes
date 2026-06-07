package com.example.superheroes.entity;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShieldProjectileEntity extends Projectile {
	private static final EntityDataAccessor<Float> DATA_ROT =
			SynchedEntityData.defineId(ShieldProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<ItemStack> DATA_STACK =
			SynchedEntityData.defineId(ShieldProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

	private static final int MAX_BOUNCES = 3;
	private static final double BOUNCE_RANGE = 8.0;
	private static final float DAMAGE = 14.0f;
	private static final double SPEED = 1.4;
	private static final int MAX_LIFETIME_TICKS = 100;
	private static final int FORCE_RETURN_TICKS = 80;

	private int bounces = 0;
	private boolean returning = false;
	private final Set<UUID> alreadyHit = new HashSet<>();
	private int lifeTicks = 0;
	private ItemStack savedStack = ItemStack.EMPTY;
	private InteractionHand savedHand = InteractionHand.OFF_HAND;
	private UUID savedOwnerUuid;

	public ShieldProjectileEntity(EntityType<? extends ShieldProjectileEntity> type, Level level) {
		super(type, level);
	}

	public static ShieldProjectileEntity throwFrom(LivingEntity owner, Level level, ItemStack savedStack, InteractionHand savedHand) {
		ShieldProjectileEntity proj = new ShieldProjectileEntity(ModEntities.SHIELD_PROJECTILE, level);
		proj.setOwner(owner);
		proj.savedOwnerUuid = owner.getUUID();
		Vec3 eye = owner.getEyePosition();
		proj.setPos(eye.x, eye.y - 0.2, eye.z);
		Vec3 dir = owner.getLookAngle().normalize().scale(SPEED);
		proj.setDeltaMovement(dir);
		proj.savedStack = savedStack == null ? ItemStack.EMPTY : savedStack;
		proj.entityData.set(DATA_STACK, proj.savedStack);
		proj.savedHand = savedHand == null ? InteractionHand.OFF_HAND : savedHand;
		return proj;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ROT, 0f);
		builder.define(DATA_STACK, ItemStack.EMPTY);
	}

	public float getRotation() {
		return this.entityData.get(DATA_ROT);
	}

	public ItemStack getShieldStack() {
		ItemStack synced = this.entityData.get(DATA_STACK);
		return synced.isEmpty() ? savedStack : synced;
	}

	@Override
	public void tick() {
		super.tick();

		this.entityData.set(DATA_ROT, (this.entityData.get(DATA_ROT) + 30f) % 360f);

		Vec3 pos = this.position();
		Vec3 motion = this.getDeltaMovement();

		if (this.level() instanceof ServerLevel server) {
			lifeTicks++;
			if (lifeTicks > MAX_LIFETIME_TICKS) {
				this.discard();
				return;
			}
			if (lifeTicks > FORCE_RETURN_TICKS) {
				returning = true;
			}

			LivingEntity owner = findOwner(server);

			if (returning && owner != null) {
				Vec3 toOwner = owner.position().add(0, owner.getBbHeight() / 2.0, 0).subtract(pos);
				double dist = toOwner.length();
				if (dist < 1.5) {
					server.playSound(null, pos.x, pos.y, pos.z,
							SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.4f);
					this.discard();
					return;
				}
				motion = toOwner.normalize().scale(SPEED * 1.3);
				this.setDeltaMovement(motion);
			}

			AABB hitBox = this.getBoundingBox().expandTowards(motion).inflate(0.4);
			LivingEntity hit = null;
			double bestDist = Double.MAX_VALUE;
			for (LivingEntity le : server.getEntitiesOfClass(LivingEntity.class, hitBox,
					e -> e.isAlive() && !e.isSpectator() && e != this.getOwner() && !alreadyHit.contains(e.getUUID()))) {
				double d = le.distanceToSqr(this);
				if (d < bestDist) {
					bestDist = d;
					hit = le;
				}
			}
			if (hit != null) {
				onHitTarget(server, hit, owner);
			}
		}

		this.setPos(pos.add(motion));

		if (this.level().isClientSide && this.tickCount % 1 == 0) {
			Level level = this.level();
			level.addParticle(ModParticles.CAP_SHIELD_TRAIL,
					this.getX(), this.getY() + 0.2, this.getZ(),
					0, 0, 0);
			if (this.tickCount % 3 == 0) {
				level.addParticle(ParticleTypes.CRIT,
						this.getX(), this.getY() + 0.2, this.getZ(),
						0, 0.05, 0);
			}
		}
	}

	private void onHitTarget(ServerLevel server, LivingEntity target, LivingEntity owner) {
		alreadyHit.add(target.getUUID());
		float dmg = DAMAGE - bounces * 2.0f;
		if (owner != null) {
			target.hurt(ModDamageTypes.capShieldThrow(server, owner), dmg);
		} else {
			target.hurt(server.damageSources().generic(), dmg);
		}
		server.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.4f, 0.7f);
		server.sendParticles(ParticleTypes.CRIT,
				target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
				14, 0.4, 0.4, 0.4, 0.2);

		bounces++;
		if (bounces >= MAX_BOUNCES) {
			returning = true;
			return;
		}

		LivingEntity nextTarget = findNextTarget(server, target);
		if (nextTarget != null) {
			Vec3 dir = nextTarget.position().add(0, nextTarget.getBbHeight() / 2, 0)
					.subtract(this.position()).normalize().scale(SPEED * 1.1);
			this.setDeltaMovement(dir);
		} else {
			returning = true;
		}
	}

	private LivingEntity findNextTarget(ServerLevel server, LivingEntity from) {
		AABB area = from.getBoundingBox().inflate(BOUNCE_RANGE);
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity le : server.getEntitiesOfClass(LivingEntity.class, area,
				e -> e.isAlive() && !e.isSpectator() && e != this.getOwner() && !alreadyHit.contains(e.getUUID()))) {
			double d = le.distanceToSqr(from);
			if (d < bestDist) {
				bestDist = d;
				best = le;
			}
		}
		return best;
	}

	@Override
	protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
		super.onHitBlock(result);
		if (!(this.level() instanceof ServerLevel server)) return;
		server.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 1.5f);
		returning = true;
		this.setDeltaMovement(this.getDeltaMovement().scale(-0.3));
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
	}

	@Override
	protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
		return entity != this.getOwner() && super.canHitEntity(entity);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Bounces", bounces);
		tag.putBoolean("Returning", returning);
		tag.putInt("LifeTicks", lifeTicks);
		tag.putString("SavedHand", savedHand.name());
		if (savedOwnerUuid != null) {
			tag.putUUID("SavedOwner", savedOwnerUuid);
		}
		if (!savedStack.isEmpty()) {
			tag.put("SavedStack", savedStack.save(this.registryAccess()));
		}
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		bounces = tag.getInt("Bounces");
		returning = tag.getBoolean("Returning");
		lifeTicks = tag.getInt("LifeTicks");
		if (tag.hasUUID("SavedOwner")) {
			savedOwnerUuid = tag.getUUID("SavedOwner");
		}
		if (tag.contains("SavedStack")) {
			savedStack = ItemStack.parseOptional(this.registryAccess(), tag.getCompound("SavedStack"));
			this.entityData.set(DATA_STACK, savedStack);
		}
		try {
			savedHand = InteractionHand.valueOf(tag.getString("SavedHand"));
		} catch (IllegalArgumentException ignored) {
			savedHand = InteractionHand.OFF_HAND;
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide && !savedStack.isEmpty()) {
			ItemStack toReturn = savedStack;
			savedStack = ItemStack.EMPTY;
			ServerPlayer sp = this.level() instanceof ServerLevel server ? findOwnerPlayer(server) : null;
			if (sp != null) {
				if (sp.getItemInHand(savedHand).isEmpty()) {
					sp.setItemInHand(savedHand, toReturn);
				} else if (!sp.getInventory().add(toReturn)) {
					sp.drop(toReturn, false);
				}
			} else if (this.level() instanceof ServerLevel server) {
				ItemEntity item = new ItemEntity(server, this.getX(), this.getY(), this.getZ(), toReturn);
				item.setDefaultPickUpDelay();
				server.addFreshEntity(item);
			}
		}
		super.remove(reason);
	}

	private LivingEntity findOwner(ServerLevel server) {
		if (this.getOwner() instanceof LivingEntity living) {
			if (savedOwnerUuid == null) {
				savedOwnerUuid = living.getUUID();
			}
			return living;
		}
		ServerPlayer player = findOwnerPlayer(server);
		if (player != null) {
			setOwner(player);
		}
		return player;
	}

	private ServerPlayer findOwnerPlayer(ServerLevel server) {
		if (savedOwnerUuid == null && this.getOwner() != null) {
			savedOwnerUuid = this.getOwner().getUUID();
		}
		return savedOwnerUuid == null ? null : server.getServer().getPlayerList().getPlayer(savedOwnerUuid);
	}
}
