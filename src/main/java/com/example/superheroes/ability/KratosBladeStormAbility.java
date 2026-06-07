package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KratosBladeStormAbility implements Ability {
	private static final int COOLDOWN_TICKS = 120;
	private static final double RADIUS = 6.0;
	private static final float CENTER_DAMAGE = 25.0f;
	private static final float EDGE_DAMAGE = 10.0f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.KRATOS_BLADE_STORM;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 100f;
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

		AABB aoe = new AABB(
				center.x - RADIUS, center.y - 2, center.z - RADIUS,
				center.x + RADIUS, center.y + 4, center.z + RADIUS);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, aoe,
				e -> e != player && e.isAlive() && !(e instanceof Player p && p.getUUID().equals(player.getUUID())))) {
			double dist = le.position().distanceTo(center);
			if (dist > RADIUS) continue;
			float falloff = (float) Math.max(0.0, 1.0 - dist / RADIUS);
			float damage = EDGE_DAMAGE + (CENTER_DAMAGE - EDGE_DAMAGE) * falloff;
			le.hurt(ModDamageTypes.kratosBlade(level, player), damage);
			le.igniteForSeconds(4f);
			Vec3 push = le.position().subtract(center).normalize().scale(2.0);
			le.setDeltaMovement(push.x, 0.6, push.z);
			le.hurtMarked = true;
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 4, 1.0, 0.5, 1.0, 0.0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.5, center.z, 200, RADIUS * 0.6, 0.5, RADIUS * 0.6, 0.3);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5, center.z, 100, RADIUS * 0.6, 0.5, RADIUS * 0.6, 0.1);

		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.7f);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.4f, 0.5f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
