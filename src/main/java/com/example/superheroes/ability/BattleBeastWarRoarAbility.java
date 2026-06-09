package com.example.superheroes.ability;

import com.example.superheroes.effect.BattleBeastCurseController;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BattleBeastWarRoarAbility implements Ability {
	private static final int COOLDOWN_TICKS = 10 * 20;
	private static final double RADIUS = 9.0;
	private static final float DAMAGE = 8.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.BATTLE_BEAST_WAR_ROAR;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 50f;
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
		Vec3 center = player.position().add(0.0, 1.0, 0.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(RADIUS), target -> isValidTarget(player, target))) {
			Vec3 away = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(center);
			double distance = away.length();
			if (distance > RADIUS || distance < 0.001) continue;
			target.hurt(level.damageSources().playerAttack(player), BattleBeastCurseController.scaleDamage(player, DAMAGE));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, true, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, true, true, true));
			Vec3 push = away.scale(1.0 / distance).scale(2.0).add(0.0, 0.35, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
			if (target instanceof ServerPlayer targetPlayer) {
				targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
			}
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
				100, RADIUS * 0.45, 0.45, RADIUS * 0.45, 0.18);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.8f, 0.75f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		return target != owner
				&& target.isAlive()
				&& !target.isSpectator()
				&& !(target instanceof Player player && player.isCreative());
	}
}
