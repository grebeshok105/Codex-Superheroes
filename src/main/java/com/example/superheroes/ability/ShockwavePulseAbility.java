package com.example.superheroes.ability;

import com.example.superheroes.physics.ShockwaveUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ShockwavePulseAbility implements Ability {
	private static final int COOLDOWN_TICKS = 120;
	private static final double RADIUS = 5.4;
	private static final float DAMAGE = 14f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SHOCKWAVE_PULSE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 45f;
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
		Vec3 center = player.position();
		ShockwaveUtil.detonate(player, center, RADIUS, DAMAGE, false);
		level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.9, center.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				center.x, center.y + 0.35, center.z,
				54, RADIUS * 0.5, 0.22, RADIUS * 0.5, 0.2);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.75f, 1.25f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
