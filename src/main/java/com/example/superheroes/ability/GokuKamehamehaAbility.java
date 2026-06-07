package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.effect.GokuKiStackController;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GokuKamehamehaAbility implements Ability {
	private static final int CHARGE_TICKS = 40;
	private static final int BEAM_TICKS = 30;
	private static final int COOLDOWN_TICKS = 200;
	private static final double RANGE = 30.0;
	private static final float BASE_DAMAGE = 14.0f;
	private static final float STACK_BONUS = 0.5f;

	private static final WeakHashMap<UUID, ActiveBeam> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GOKU_KAMEHAMEHA;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 60f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId()) && !ACTIVE.containsKey(player.getUUID());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		int stacks = GokuKiStackController.consume(player);
		float multiplier = 1.0f + STACK_BONUS * stacks;
		ACTIVE.put(player.getUUID(), new ActiveBeam(CHARGE_TICKS + BEAM_TICKS, stacks, multiplier, new HashSet<>()));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6f, 0.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.7f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveBeam ab = ACTIVE.get(player.getUUID());
		if (ab == null) return;
		ServerLevel level = player.serverLevel();
		int remaining = ab.ticksLeft;
		int phase = (CHARGE_TICKS + BEAM_TICKS) - remaining;

		if (phase < CHARGE_TICKS) {
			Vec3 hand = player.getEyePosition().add(player.getViewVector(1f).scale(0.6));
			float radius = 0.3f + (float) phase / CHARGE_TICKS * 1.4f;
			for (int i = 0; i < 4; i++) {
				double angle = Math.random() * Math.PI * 2;
				double r = Math.random() * radius;
				level.sendParticles(ModParticles.GOKU_KAMEHAMEHA_CORE,
						hand.x + Math.cos(angle) * r,
						hand.y + (Math.random() - 0.5) * radius,
						hand.z + Math.sin(angle) * r,
						1, 0, 0, 0, 0.0);
			}
			if (phase % 6 == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.5f + phase * 0.02f);
			}
		} else {
			int beamPhase = phase - CHARGE_TICKS;
			if (beamPhase == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.9f);
			}
			fireBeam(player, ab);
		}

		ab.ticksLeft--;
		if (ab.ticksLeft <= 0) {
			ACTIVE.remove(player.getUUID());
		}
	}

	private static void fireBeam(ServerPlayer player, ActiveBeam ab) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult bh = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 actualEnd = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;

		float damage = BASE_DAMAGE * ab.multiplier;
		AABB box = new AABB(eye, actualEnd).inflate(1.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator() && distanceToBeam(e, eye, dir) < 1.5)) {
			if (ab.alreadyHit.add(target.getUUID())) {
				target.hurt(ModDamageTypes.gokuKamehameha(level, player), damage);
				target.igniteForSeconds(2f);
			}
		}

		double dist = eye.distanceTo(actualEnd);
		int steps = (int) Math.max(8, dist * 2);
		for (int i = 0; i < steps; i++) {
			double t = (double) i / steps;
			Vec3 p = eye.add(dir.scale(t * dist));
			level.sendParticles(ModParticles.GOKU_KAMEHAMEHA_CORE,
					p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.0);
			level.sendParticles(ModParticles.GOKU_KAMEHAMEHA_TRAIL,
					p.x, p.y, p.z, 1, 0.4, 0.4, 0.4, 0.02);
			level.sendParticles(ParticleTypes.END_ROD,
					p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
		}
		level.sendParticles(ParticleTypes.EXPLOSION,
				actualEnd.x, actualEnd.y, actualEnd.z, 2, 0.5, 0.5, 0.5, 0.0);
	}

	private static double distanceToBeam(Entity entity, Vec3 origin, Vec3 dir) {
		Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2.0, 0).subtract(origin);
		double along = toEntity.dot(dir);
		Vec3 onLine = origin.add(dir.scale(Math.max(0, along)));
		return entity.position().add(0, entity.getBbHeight() / 2.0, 0).distanceTo(onLine);
	}

	private static final class ActiveBeam {
		int ticksLeft;
		final int stacks;
		final float multiplier;
		final Set<UUID> alreadyHit;

		ActiveBeam(int ticksLeft, int stacks, float multiplier, Set<UUID> alreadyHit) {
			this.ticksLeft = ticksLeft;
			this.stacks = stacks;
			this.multiplier = multiplier;
			this.alreadyHit = alreadyHit;
		}
	}
}
