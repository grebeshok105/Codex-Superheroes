package io.github.grebeshok105.codex.hero.homelander.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class HandClapAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("hand_clap");
	private static final double RANGE = 18.0;
	private static final double CONE_HALF_ANGLE_COS = Math.cos(Math.toRadians(45.0));
	private static final float DAMAGE = 20.0f;
	private static final double KNOCKBACK = 3.5;
	private static final int COOLDOWN_TICKS = 240;

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
		return 50f;
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
		List<Entity> hits = level.getEntities(player, area,
				e -> e instanceof LivingEntity le && TargetFilters.hostileTo(player).test(le));
		for (Entity entity : hits) {
			Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = toTarget.length();
			if (dist > RANGE || dist < 0.001) continue;
			Vec3 norm = toTarget.normalize();
			if (norm.dot(forward) < CONE_HALF_ANGLE_COS) continue;
			entity.hurt(level.damageSources().playerAttack(player), DAMAGE);
			Vec3 push = forward.scale(KNOCKBACK);
			entity.push(push.x, 0.6, push.z);
			entity.hurtMarked = true;
		}

		// The whole clap presentation is event-driven now: ClapFx owns
		// homelander.hand_clap, the clap clip, the flash between the hands,
		// the forward cone, the distortion pulse and the proximity shake.
		VfxFx.event(player, HomelanderVfxIds.CLAP, origin, origin.add(forward.scale(RANGE)), 1f);

		AbilityCooldowns.setCooldownTicks(player, ID, COOLDOWN_TICKS);
		return true;
	}
}
