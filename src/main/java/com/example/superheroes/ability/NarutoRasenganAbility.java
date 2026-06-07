package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.WeakHashMap;

public final class NarutoRasenganAbility implements Ability {
	private static final int CHARGE_TICKS = 30;
	private static final int WINDOW_TICKS = 100;
	private static final int COOLDOWN_TICKS = 200;
	private static final float DAMAGE = 35.0f;
	private static final double AOE_RADIUS = 3.0;
	private static final double STRIKE_RANGE = 4.0;

	private static final WeakHashMap<UUID, ActiveRasengan> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.NARUTO_RASENGAN;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 100f;
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
		ACTIVE.put(player.getUUID(), new ActiveRasengan(CHARGE_TICKS + WINDOW_TICKS, false));
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.5f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveRasengan ar = ACTIVE.get(player.getUUID());
		if (ar == null) return;
		ServerLevel level = player.serverLevel();
		int total = CHARGE_TICKS + WINDOW_TICKS;
		int phase = total - ar.ticksLeft;

		Vec3 hand = player.position().add(player.getLookAngle().scale(0.6))
				.add(0, 1.2, 0);

		if (phase < CHARGE_TICKS) {
			float t = (float) phase / CHARGE_TICKS;
			double radius = 0.2 + t * 0.4;
			for (int i = 0; i < 6; i++) {
				double angle = (player.tickCount * 0.6 + i * Math.PI / 3) % (Math.PI * 2);
				double r = radius;
				level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
						hand.x + Math.cos(angle) * r,
						hand.y + Math.sin(angle * 2) * r * 0.3,
						hand.z + Math.sin(angle) * r,
						1, 0, 0, 0, 0.0);
			}
			if (phase == CHARGE_TICKS - 1) {
				level.playSound(null, hand.x, hand.y, hand.z,
						SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 1.2f);
			}
		} else if (!ar.detonated) {
			// Ready to strike
			for (int i = 0; i < 8; i++) {
				double angle = (player.tickCount * 0.8 + i * Math.PI / 4) % (Math.PI * 2);
				level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
						hand.x + Math.cos(angle) * 0.6,
						hand.y + Math.sin(angle * 2) * 0.2,
						hand.z + Math.sin(angle) * 0.6,
						1, 0, 0, 0, 0.0);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
						hand.x + Math.cos(angle) * 0.6,
						hand.y + Math.sin(angle * 2) * 0.2,
						hand.z + Math.sin(angle) * 0.6,
						1, 0, 0, 0, 0.0);
			}

			Vec3 forward = hand.add(player.getLookAngle().scale(STRIKE_RANGE));
			AABB box = new AABB(hand, forward).inflate(1.5);
			LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, box,
					e -> e != player && e.isAlive() && !e.isSpectator()).stream()
					.findFirst().orElse(null);
			if (target != null) {
				detonate(player, target, ar);
			}
		}

		ar.ticksLeft--;
		if (ar.ticksLeft <= 0) {
			ACTIVE.remove(player.getUUID());
		}
	}

	private static void detonate(ServerPlayer player, LivingEntity primary, ActiveRasengan ar) {
		ar.detonated = true;
		ServerLevel level = player.serverLevel();
		Vec3 hit = primary.position().add(0, primary.getBbHeight() / 2, 0);

		primary.hurt(ModDamageTypes.narutoRasengan(level, player), DAMAGE);
		Vec3 push = player.getLookAngle();
		primary.setDeltaMovement(push.x * 1.6, 0.5, push.z * 1.6);
		primary.hurtMarked = true;

		AABB aoe = new AABB(hit.x - AOE_RADIUS, hit.y - AOE_RADIUS, hit.z - AOE_RADIUS,
				hit.x + AOE_RADIUS, hit.y + AOE_RADIUS, hit.z + AOE_RADIUS);
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, aoe,
				e -> e != player && e != primary && e.isAlive() && !e.isSpectator())) {
			nearby.hurt(ModDamageTypes.narutoRasengan(level, player), DAMAGE * 0.4f);
			Vec3 away = nearby.position().subtract(hit).normalize();
			nearby.push(away.x * 0.8, 0.3, away.z * 0.8);
		}

		level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
				hit.x, hit.y, hit.z, 40, AOE_RADIUS * 0.5, AOE_RADIUS * 0.5, AOE_RADIUS * 0.5, 0.4);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				hit.x, hit.y, hit.z, 30, AOE_RADIUS * 0.4, AOE_RADIUS * 0.4, AOE_RADIUS * 0.4, 0.2);
		level.sendParticles(ParticleTypes.EXPLOSION,
				hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
		level.playSound(null, hit.x, hit.y, hit.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6f, 0.6f);
		level.playSound(null, hit.x, hit.y, hit.z,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.4f);
	}

	private static final class ActiveRasengan {
		int ticksLeft;
		boolean detonated;

		ActiveRasengan(int ticksLeft, boolean detonated) {
			this.ticksLeft = ticksLeft;
			this.detonated = detonated;
		}
	}
}
