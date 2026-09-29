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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class StunningRoarAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("stunning_roar");
	private static final double RADIUS = 12.0;
	private static final float DAMAGE = 14.0f;
	private static final int DARKNESS_DURATION = 80;
	private static final int COOLDOWN_TICKS = 160;

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
		return 30f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		Vec3 forward = player.getViewVector(1f).normalize();
		Vec3 mouth = player.getEyePosition().add(forward.scale(0.6));

		AABB box = new AABB(origin, origin).inflate(RADIUS);
		List<Entity> hits = level.getEntities(player, box,
				e -> e instanceof LivingEntity le && TargetFilters.hostileTo(player).test(le));
		for (Entity e : hits) {
			double d = e.position().distanceTo(origin);
			if (d > RADIUS) continue;
			e.hurt(level.damageSources().playerAttack(player), DAMAGE);
			Vec3 push = e.position().subtract(origin).normalize().scale(0.6);
			e.push(push.x, 0.25, push.z);
			e.hurtMarked = true;
			if (e instanceof LivingEntity le) {
				le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_DURATION, 0, false, false, true));
			}
		}

		// The whole roar presentation is event-driven now: RoarFx owns both
		// contract sounds (homelander.roar + homelander.roar.deep layered as
		// before), the roar clip, the mouth-anchored sound-wave cone, the
		// distortion, the dust lift and the rumble shake.
		VfxFx.event(player, HomelanderVfxIds.ROAR, mouth, mouth.add(forward.scale(RADIUS)), 1f);

		AbilityCooldowns.setCooldownTicks(player, ID, COOLDOWN_TICKS);
		return true;
	}
}
