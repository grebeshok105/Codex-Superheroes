package com.example.superheroes.ability;

import com.example.superheroes.effect.SungJinwooController;
import com.example.superheroes.entity.ShadowSoldierEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Shadow Exchange — Сон и ближайшая тень меняются местами. 0.5с неуязвимости после.
 *  - Cost: 15 ENERGY
 *  - CD: 200t (10с)
 */
public final class ShadowExchangeAbility implements Ability {
	private static final int COOLDOWN_TICKS = 200;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SHADOW_EXCHANGE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 15f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !SungJinwooController.aliveShadows(player).isEmpty();
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		List<ShadowSoldierEntity> shadows = SungJinwooController.aliveShadows(player);
		if (shadows.isEmpty()) return false;

		ShadowSoldierEntity nearest = shadows.stream()
				.min(Comparator.comparingDouble(s -> s.distanceToSqr(player)))
				.orElse(null);
		if (nearest == null) return false;

		ServerLevel level = player.serverLevel();
		Vec3 playerPos = player.position();
		Vec3 shadowPos = nearest.position();

		// Свап позиций
		nearest.teleportTo(playerPos.x, playerPos.y, playerPos.z);
		player.teleportTo(shadowPos.x, shadowPos.y, shadowPos.z);

		// 0.5с неуязвимости
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, true, false, false));

		level.sendParticles(ParticleTypes.PORTAL, playerPos.x, playerPos.y + 1, playerPos.z, 40, 0.4, 1.0, 0.4, 0.5);
		level.sendParticles(ParticleTypes.PORTAL, shadowPos.x, shadowPos.y + 1, shadowPos.z, 40, 0.4, 1.0, 0.4, 0.5);
		level.playSound(null, playerPos.x, playerPos.y, playerPos.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
		level.playSound(null, shadowPos.x, shadowPos.y, shadowPos.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.9f);

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.SHADOW_EXCHANGE, COOLDOWN_TICKS);
		return true;
	}
}
