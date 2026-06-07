package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class DoomsdayRoarAbility implements Ability {
	private static final int COOLDOWN_TICKS = 120;
	private static final double RADIUS = 9.0;
	private static final float DAMAGE = 8.0f;
	private static final double KNOCKBACK = 2.6;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.DOOMSDAY_ROAR;
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
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position().add(0, player.getBbHeight() * 0.5, 0);

		AABB area = new AABB(origin, origin).inflate(RADIUS);
		List<Entity> targets = level.getEntities(player, area,
				e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity);

		for (Entity entity : targets) {
			Vec3 to = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = to.length();
			if (dist > RADIUS || dist < 0.001) continue;
			Vec3 push = to.normalize().scale(KNOCKBACK);
			entity.hurt(ModDamageTypes.doomsdayRoar(level, player), DAMAGE);
			entity.push(push.x, 0.6, push.z);
			entity.hurtMarked = true;
			if (entity instanceof LivingEntity living) {
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true, true));
				living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true, true));
			}
		}

		level.playSound(null, origin.x, origin.y, origin.z,
				com.example.superheroes.sound.ModSounds.DOOMSDAY_ROAR, SoundSource.PLAYERS, 2.0f, 0.9f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.6f);

		level.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y, origin.z,
				100, RADIUS * 0.45, 0.5, RADIUS * 0.45, 0.2);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
