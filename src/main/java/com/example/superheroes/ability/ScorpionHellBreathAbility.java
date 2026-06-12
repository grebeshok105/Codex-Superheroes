package com.example.superheroes.ability;

import com.example.superheroes.effect.ScorpionController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Breath of the Netherrealm — Scorpion tears off his mask and unleashes a
 * 2.5 second flamethrower cone (channel is ticked by ScorpionController).
 */
public final class ScorpionHellBreathAbility implements Ability {
	private static final int COOLDOWN_TICKS = 30 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SCORPION_HELL_BREATH;
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
		return !AbilityCooldowns.isOnCooldown(player, getId())
				&& !ScorpionController.isBreathing(player);
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f);

		ScorpionController.startBreath(player);

		level.sendParticles(ParticleTypes.FLAME,
				eye.x + forward.x, eye.y - 0.2, eye.z + forward.z, 30, 0.3, 0.25, 0.3, 0.08);
		level.sendParticles(ParticleTypes.LAVA,
				eye.x + forward.x, eye.y - 0.2, eye.z + forward.z, 4, 0.25, 0.2, 0.25, 0.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.4f, 0.5f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
