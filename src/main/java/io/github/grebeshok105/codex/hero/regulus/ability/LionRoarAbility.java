package io.github.grebeshok105.codex.hero.regulus.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class LionRoarAbility implements Ability {
	private static final double RANGE = 18.0;
	private static final double CONE_HALF_ANGLE_COS = Math.cos(Math.toRadians(45.0));
	private static final float DAMAGE = 14.0f;
	private static final double KNOCKBACK = 3.0;

	public static final ResourceLocation ID = ModId.of("lion_roar");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 150f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();

		AABB area = new AABB(origin, origin).inflate(RANGE);
		List<Entity> candidates = level.getEntities(player, area,
				e -> e instanceof LivingEntity le && TargetFilters.hostileTo(player).test(le));

		for (Entity entity : candidates) {
			Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = toTarget.length();
			if (dist > RANGE || dist < 0.001) {
				continue;
			}
			Vec3 toNorm = toTarget.normalize();
			double dot = toNorm.dot(forward);
			if (dot < CONE_HALF_ANGLE_COS) {
				continue;
			}
			entity.hurt(RegulusDamageTypes.lionRoar(level, player), DAMAGE);
			Vec3 push = forward.scale(KNOCKBACK);
			entity.push(push.x, 0.5, push.z);
			entity.hurtMarked = true;
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.1f);

		Vec3 cloudOrigin = origin.add(forward.scale(2.0));
		level.sendParticles(ParticleTypes.SONIC_BOOM,
				cloudOrigin.x, cloudOrigin.y, cloudOrigin.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(new ShriekParticleOption(0),
				cloudOrigin.x, cloudOrigin.y, cloudOrigin.z, 1, 0.0, 0.0, 0.0, 0.0);
		// Golden shockwave wake: a staggered cone of embers along the roar path.
		for (double d = 2.0; d <= 7.0; d += 2.5) {
			Vec3 p = cloudOrigin.add(forward.scale(d));
			level.sendParticles(ParticleTypes.END_ROD,
					p.x, p.y, p.z, 8, 0.35 + d * 0.06, 0.25, 0.35 + d * 0.06, 0.22);
		}
		RegulusFx.flash(player);
		RegulusFx.ringBurst(player);
		return true;
	}
}
