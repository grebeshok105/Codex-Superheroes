package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class NarutoRasenshurikenAbility implements Ability {
	private static final int CHARGE_TICKS = 24;
	private static final int COOLDOWN_TICKS = 600;
	private static final int MAX_LIFE_TICKS = 24;
	private static final double SPEED = 1.25;
	private static final double RADIUS = 3.0;
	private static final float DAMAGE = 18f;
	private static final Map<UUID, ActiveRasenshuriken> ACTIVE = new WeakHashMap<>();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.NARUTO_RASENSHURIKEN;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 120f;
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
		ACTIVE.put(player.getUUID(), new ActiveRasenshuriken(player));
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 1.2f, 1.5f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		ActiveRasenshuriken active = ACTIVE.get(player.getUUID());
		if (active == null) {
			return;
		}
		if (!active.tick(player)) {
			ACTIVE.remove(player.getUUID());
		}
	}

	private static final class ActiveRasenshuriken {
		private final UUID ownerId;
		private Vec3 pos;
		private Vec3 dir;
		private int chargeTicks;
		private int lifeTicks;
		private boolean launched;

		private ActiveRasenshuriken(ServerPlayer player) {
			this.ownerId = player.getUUID();
			this.pos = player.position().add(0, 1.4, 0);
			this.dir = player.getLookAngle();
			this.chargeTicks = CHARGE_TICKS;
		}

		private boolean tick(ServerPlayer player) {
			if (!player.isAlive() || !player.getUUID().equals(ownerId)) {
				return false;
			}
			ServerLevel level = player.serverLevel();
			if (!launched) {
				pos = player.position().add(player.getLookAngle().scale(1.0)).add(0, 1.4, 0);
				dir = player.getLookAngle().normalize();
				level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
						pos.x, pos.y, pos.z, 22, 0.45, 0.45, 0.45, 0.08);
				level.sendParticles(ParticleTypes.GUST,
						pos.x, pos.y, pos.z, 8, 0.35, 0.35, 0.35, 0.05);
				chargeTicks--;
				if (chargeTicks <= 0) {
					launched = true;
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.4f, 1.2f);
				}
				return true;
			}
			lifeTicks++;
			pos = pos.add(dir.scale(SPEED));
			level.sendParticles(ModParticles.NARUTO_RASENGAN_SWIRL,
					pos.x, pos.y, pos.z, 28, 0.7, 0.7, 0.7, 0.1);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					pos.x, pos.y, pos.z, 4, 0.8, 0.2, 0.8, 0.0);
			List<LivingEntity> victims = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
					new AABB(pos, pos).inflate(RADIUS),
					e -> e != player && e.isAlive() && !e.isSpectator()));
			if (!victims.isEmpty()) {
				for (LivingEntity victim : victims) {
					victim.invulnerableTime = 0;
					victim.hurt(ModDamageTypes.narutoRasenshuriken(level, player), DAMAGE);
					victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, true, true, true));
					Vec3 push = victim.position().subtract(pos).normalize().scale(0.6);
					victim.push(push.x, 0.25, push.z);
				}
				level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
						pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
				level.playSound(null, pos.x, pos.y, pos.z,
						SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.6f, 0.8f);
				return false;
			}
			return lifeTicks < MAX_LIFE_TICKS;
		}
	}
}
