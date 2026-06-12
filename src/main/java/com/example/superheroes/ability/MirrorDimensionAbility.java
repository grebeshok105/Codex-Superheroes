package com.example.superheroes.ability;

import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.network.MirrorDimensionS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Doctor Strange — Mirror Dimension. Toggle: the aimed player (or Strange
 * himself when nobody is aimed at — self-test mode) gets the Acid Shaders
 * "round world" spherical-inversion warp on their client until the toggle
 * ends. Pure sensory trap: no damage, no movement control.
 */
public final class MirrorDimensionAbility implements Ability {
	/** Acid Shaders option MODE: 4 = "Uptown" inversion (world curls up around the player). */
	public static final int ACID_MODE = 4;
	/** Acid Shaders option J: sphere scale, smaller = tighter loop. */
	public static final int ACID_SCALE = 16;

	private static final double RANGE = 32.0;
	private static final double AIM_DOT_MIN = 0.97;
	private static final int COOLDOWN_TICKS = 10 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.MIRROR_DIMENSION;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 60f;
	}

	@Override
	public float costPerTick() {
		return 0.5f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerPlayer victim = findAimedPlayer(player);
		if (victim == null) {
			victim = player; // self-test mode
		}
		if (!ServerPlayNetworking.canSend(victim, MirrorDimensionS2CPayload.TYPE)) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.no_mod", victim.getName())
							.withStyle(ChatFormatting.RED), true);
			return false;
		}
		if (!MirrorDimensionController.start(player, victim, ACID_MODE, ACID_SCALE)) {
			return false;
		}
		spawnCastParticles(victim);
		return true;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		MirrorDimensionController.stop(player, true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
	}

	private static ServerPlayer findAimedPlayer(ServerPlayer caster) {
		Vec3 eye = caster.getEyePosition();
		Vec3 look = caster.getLookAngle();
		ServerPlayer best = null;
		double bestDot = AIM_DOT_MIN;
		for (ServerPlayer candidate : caster.serverLevel().players()) {
			if (candidate == caster || candidate.isSpectator() || candidate.isDeadOrDying()) {
				continue;
			}
			Vec3 to = candidate.getEyePosition().subtract(eye);
			double distance = to.length();
			if (distance > RANGE || distance < 0.01) {
				continue;
			}
			double dot = to.normalize().dot(look);
			if (dot >= bestDot && caster.hasLineOfSight(candidate)) {
				bestDot = dot;
				best = candidate;
			}
		}
		return best;
	}

	private static void spawnCastParticles(ServerPlayer victim) {
		ServerLevel level = victim.serverLevel();
		level.sendParticles(ParticleTypes.REVERSE_PORTAL,
				victim.getX(), victim.getY() + 1.0, victim.getZ(),
				80, 0.8, 1.0, 0.8, 0.05);
	}
}
