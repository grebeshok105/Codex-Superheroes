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

/**
 * Senpou: Cho Oodama Rasengan — Sage Art: Super Big Ball Rasengan.
 * Куда сильнее обычного Rasengan: больший радиус AoE, больше урон, дольше зарядка.
 *
 *  - Стоимость: 200 ENERGY на активацию.
 *  - Cooldown: 16 секунд (320 тиков).
 *  - Зарядка: 50 тиков (2.5с) — за это время нельзя ходить очень быстро (Slowness II у самого Наруто).
 *  - После зарядки — авто-удар по ближайшей цели в 6 блоках, AoE 6.0, урон 50.
 *  - Источник урона: {@link ModDamageTypes#narutoRasengan} (тот же тип, что и у обычного).
 */
public final class NarutoOodamaRasenganAbility implements Ability {
	private static final int CHARGE_TICKS = 50;
	private static final int WINDOW_TICKS = 100;
	private static final int COOLDOWN_TICKS = 320;
	private static final float DAMAGE = 50.0f;
	private static final double AOE_RADIUS = 6.0;
	private static final double STRIKE_RANGE = 6.0;

	private static final WeakHashMap<UUID, ActiveCharge> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.NARUTO_OODAMA_RASENGAN;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 200f;
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
		ACTIVE.put(player.getUUID(), new ActiveCharge(CHARGE_TICKS + WINDOW_TICKS, false));
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.4f, 0.8f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveCharge ar = ACTIVE.get(player.getUUID());
		if (ar == null) return;
		ServerLevel level = player.serverLevel();
		int total = CHARGE_TICKS + WINDOW_TICKS;
		int phase = total - ar.ticksLeft;

		Vec3 hand = player.position().add(player.getLookAngle().scale(0.8))
				.add(0, 1.3, 0);

		if (phase < CHARGE_TICKS) {
			float t = (float) phase / CHARGE_TICKS;
			double radius = 0.4 + t * 1.0;
			for (int i = 0; i < 12; i++) {
				double angle = (player.tickCount * 0.5 + i * Math.PI / 6) % (Math.PI * 2);
				level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
						hand.x + Math.cos(angle) * radius,
						hand.y + Math.sin(angle * 2) * radius * 0.4,
						hand.z + Math.sin(angle) * radius,
						1, 0, 0, 0, 0.0);
			}
			if (phase % 6 == 0) {
				level.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 4, 0.4, 0.2, 0.4, 0.04);
			}
			if (phase == CHARGE_TICKS - 1) {
				level.playSound(null, hand.x, hand.y, hand.z,
						SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6f, 0.6f);
			}
		} else if (!ar.detonated) {
			for (int i = 0; i < 16; i++) {
				double angle = (player.tickCount * 0.7 + i * Math.PI / 8) % (Math.PI * 2);
				level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
						hand.x + Math.cos(angle) * 1.4,
						hand.y + Math.sin(angle * 2) * 0.3,
						hand.z + Math.sin(angle) * 1.4,
						1, 0, 0, 0, 0.0);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
						hand.x + Math.cos(angle) * 1.4,
						hand.y + Math.sin(angle * 2) * 0.3,
						hand.z + Math.sin(angle) * 1.4,
						1, 0, 0, 0, 0.0);
			}

			Vec3 forward = hand.add(player.getLookAngle().scale(STRIKE_RANGE));
			AABB box = new AABB(hand, forward).inflate(2.5);
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

	private static void detonate(ServerPlayer player, LivingEntity primary, ActiveCharge ar) {
		ar.detonated = true;
		ServerLevel level = player.serverLevel();
		Vec3 hit = primary.position().add(0, primary.getBbHeight() / 2, 0);

		primary.hurt(ModDamageTypes.narutoRasengan(level, player), DAMAGE);
		Vec3 push = player.getLookAngle();
		primary.setDeltaMovement(push.x * 2.4, 0.7, push.z * 2.4);
		primary.hurtMarked = true;

		AABB aoe = new AABB(hit.x - AOE_RADIUS, hit.y - AOE_RADIUS, hit.z - AOE_RADIUS,
				hit.x + AOE_RADIUS, hit.y + AOE_RADIUS, hit.z + AOE_RADIUS);
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, aoe,
				e -> e != player && e != primary && e.isAlive() && !e.isSpectator())) {
			nearby.hurt(ModDamageTypes.narutoRasengan(level, player), DAMAGE * 0.5f);
			Vec3 away = nearby.position().subtract(hit).normalize();
			nearby.push(away.x * 1.4, 0.5, away.z * 1.4);
			nearby.hurtMarked = true;
		}

		level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
				hit.x, hit.y, hit.z, 80, AOE_RADIUS * 0.6, AOE_RADIUS * 0.6, AOE_RADIUS * 0.6, 0.5);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				hit.x, hit.y, hit.z, 60, AOE_RADIUS * 0.5, AOE_RADIUS * 0.5, AOE_RADIUS * 0.5, 0.3);
		level.sendParticles(ParticleTypes.EXPLOSION,
				hit.x, hit.y, hit.z, 4, AOE_RADIUS * 0.4, AOE_RADIUS * 0.4, AOE_RADIUS * 0.4, 0);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.PLAYERS, 1.8f, 0.6f);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.WIND_CHARGE_BURST,
				SoundSource.PLAYERS, 1.6f, 0.8f);
	}

	private static final class ActiveCharge {
		int ticksLeft;
		boolean detonated;

		ActiveCharge(int ticksLeft, boolean detonated) {
			this.ticksLeft = ticksLeft;
			this.detonated = detonated;
		}
	}
}
