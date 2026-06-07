package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.physics.ShockwaveUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class DoomsdaySmashAbility implements Ability {
	private static final int COOLDOWN_TICKS = 80;
	private static final double RADIUS = 7.0;
	private static final float DAMAGE = 16.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.DOOMSDAY_SMASH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 80f;
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
		Vec3 origin = player.position();

		ShockwaveUtil.detonate(player, origin, RADIUS, DAMAGE, false,
				ModDamageTypes.doomsdaySmash(level, player));

		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6f, 0.5f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 0.7f);

		level.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y + 0.4, origin.z,
				4, 0.8, 0.2, 0.8, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 0.2, origin.z,
				60, RADIUS * 0.4, 0.3, RADIUS * 0.4, 0.08);
		level.sendParticles(ParticleTypes.POOF, origin.x, origin.y + 0.2, origin.z,
				40, RADIUS * 0.4, 0.2, RADIUS * 0.4, 0.06);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
