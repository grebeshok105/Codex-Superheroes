package com.example.superheroes.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class ScaramoucheWindPrisonAbility implements Ability {
	private static final int DURATION_TICKS = 8 * 20;
	private static final int COOLDOWN_TICKS = 14 * 20;
	private static final double RANGE = 18.0;
	private static final double RADIUS = 6.0;
	private static final float TICK_DAMAGE = 3.0f;
	private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(new Vector3f(0.26f, 1.0f, 0.82f), 1.3f);
	private static final DustParticleOptions ELECTRO_DUST = new DustParticleOptions(new Vector3f(0.56f, 0.36f, 1.0f), 1.0f);
	private static final Map<UUID, ActiveZone> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCARAMOUCHE_WIND_PRISON;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 45f;
	}

	@Override
	public float costPerTick() {
		return 1.2f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();
		if (forward.lengthSqr() < 1.0e-4) {
			forward = new Vec3(0.0, 0.0, 1.0);
		}
		Vec3 end = eye.add(forward.scale(RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 center = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : eye.add(forward.scale(10.0));
		ACTIVE.put(player.getUUID(), new ActiveZone(center, level.getGameTime() + DURATION_TICKS));

		level.sendParticles(ANEMO_DUST,
				center.x, center.y + 1.0, center.z, 120, RADIUS * 0.28, 0.8, RADIUS * 0.28, 0.0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				center.x, center.y + 1.0, center.z, 55, RADIUS * 0.25, 0.7, RADIUS * 0.25, 0.08);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.45f);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.35f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		ActiveZone zone = ACTIVE.get(player.getUUID());
		if (zone == null || player.serverLevel().getGameTime() >= zone.expireAt()) {
			AbilityRouter.deactivate(player, getId());
			return;
		}

		ServerLevel level = player.serverLevel();
		Vec3 center = zone.center();
		AABB area = new AABB(center, center).inflate(RADIUS, 4.0, RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
				target -> isValidTarget(player, target))) {
			Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			Vec3 toCenter = center.add(0.0, 1.0, 0.0).subtract(targetCenter);
			double distance = toCenter.length();
			if (distance > RADIUS + 1.5 || distance < 0.001) continue;

			Vec3 pull = toCenter.scale(0.075).add(0.0, 0.025, 0.0);
			target.setDeltaMovement(target.getDeltaMovement().add(pull));
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer && player.tickCount % 4 == 0) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, true, true, true));
			if (player.tickCount % 10 == 0) {
				target.invulnerableTime = 0;
				target.hurt(level.damageSources().magic(), TICK_DAMAGE);
				target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0, true, true, true));
				level.sendParticles(ELECTRO_DUST,
						targetCenter.x, targetCenter.y, targetCenter.z, 18, 0.3, 0.35, 0.3, 0.0);
			}
		}

		if (player.tickCount % 5 == 0) {
			spawnRing(level, center);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		ActiveZone zone = ACTIVE.remove(player.getUUID());
		if (zone == null) {
			return;
		}
		Vec3 center = zone.center();
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.CLOUD,
				center.x, center.y + 0.8, center.z, 36, RADIUS * 0.25, 0.4, RADIUS * 0.25, 0.08);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	private static void spawnRing(ServerLevel level, Vec3 center) {
		for (int i = 0; i < 24; i++) {
			double angle = Math.PI * 2.0 * i / 24.0;
			double x = center.x + Math.cos(angle) * RADIUS;
			double z = center.z + Math.sin(angle) * RADIUS;
			level.sendParticles(ANEMO_DUST, x, center.y + 0.75, z, 1, 0.04, 0.2, 0.04, 0.0);
			if (i % 4 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, center.y + 0.85, z, 1, 0.05, 0.15, 0.05, 0.02);
			}
		}
		level.sendParticles(ParticleTypes.CLOUD,
				center.x, center.y + 0.5, center.z, 8, RADIUS * 0.22, 0.25, RADIUS * 0.22, 0.04);
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}

	private record ActiveZone(Vec3 center, long expireAt) {
	}
}
