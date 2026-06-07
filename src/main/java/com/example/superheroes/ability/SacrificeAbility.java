package com.example.superheroes.ability;

import com.example.superheroes.effect.SungJinwooController;
import com.example.superheroes.entity.ShadowSoldierEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Sacrifice — все тени взрываются, нанося 8 HP в радиусе 4 блоков (не ломают блоки).
 *  - Cost: 30 ENERGY
 *  - CD: 1200t (60с)
 *  - После активации армия = 0.
 */
public final class SacrificeAbility implements Ability {
	private static final int COOLDOWN_TICKS = 1200;
	private static final double EXPLOSION_RADIUS = 4.0;
	private static final float EXPLOSION_DAMAGE = 8.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SACRIFICE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 30f;
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

		ServerLevel level = player.serverLevel();
		for (ShadowSoldierEntity shadow : shadows) {
			AABB box = shadow.getBoundingBox().inflate(EXPLOSION_RADIUS);
			List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
					e -> e != player && !(e instanceof ShadowSoldierEntity) && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())));
			for (LivingEntity hit : hits) {
				if (hit.distanceToSqr(shadow) > EXPLOSION_RADIUS * EXPLOSION_RADIUS) continue;
				hit.hurt(level.damageSources().playerAttack(player), EXPLOSION_DAMAGE);
			}
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, shadow.getX(), shadow.getY() + 0.5, shadow.getZ(),
					60, 0.6, 0.6, 0.6, 0.2);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, shadow.getX(), shadow.getY() + 0.5, shadow.getZ(),
					40, 0.6, 0.6, 0.6, 0.05);
			level.playSound(null, shadow.getX(), shadow.getY(), shadow.getZ(),
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
			shadow.discard();
		}

		SungJinwooController.disbandAll(player);
		AbilityCooldowns.setCooldownTicks(player, AbilityIds.SACRIFICE, COOLDOWN_TICKS);
		return true;
	}
}
