package io.github.grebeshok105.codex.hero.regulus.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ManiaOfGreedAbility implements Ability {
	private static final double RAY_RANGE = 100.0;
	public static final int COOLDOWN_TICKS = 500;
	/** Energy drained per tick while a magnet is held — starts only once the cast fires. */
	private static final float MAGNET_DRAIN_PER_TICK = 8f;
	/** The authored grab frame: the mania clip's magnet moment at ~0.94 s. */
	private static final int CAST_FIRE_TICKS = 19;
	/** The clip tail: the cast record lives this long even though nothing else fires. */
	private static final int CAST_UNTIL_TICKS = 42;

	public static final ResourceLocation ID = ModId.of("mania_of_greed");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		// The 8/tick drain cannot live here: costPerTick is parameterless and would
		// charge through the 19-tick windup. onTickActive drains via the controller,
		// which no-ops until the fire tick created a magnet.
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		// Only opens the cast: the magnet does not choose a victim until the fire
		// tick, so an early toggle-off or damage interrupt aborts for free.
		boolean started = RegulusCastState.startCast(player, new RegulusCastState.Spec(
				ID, CAST_FIRE_TICKS, CAST_UNTIL_TICKS, 0f, COOLDOWN_TICKS, true,
				() -> onMagnetFire(player), () -> {
				}));
		if (!started) {
			return false;
		}
		Vec3 origin = player.position();
		VfxFx.event(player, RegulusVfxIds.ANIM_MANIA_OF_GREED_CAST, origin, origin, 1f);
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.6f);
		return true;
	}

	/**
	 * The authored grab moment. A whiff (nobody in the cone) still costs the cast
	 * machine's 500-tick cooldown but leaves the victim pool untouched.
	 */
	private static void onMagnetFire(ServerPlayer player) {
		LivingEntity victim = findTarget(player);
		if (victim == null) {
			AbilityRouter.deactivate(player, ID);
			return;
		}
		RegulusGreedController.startMagnet(player, victim);
		player.serverLevel().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
				SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.7f);
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		RegulusGreedController.drainMagnet(player, MAGNET_DRAIN_PER_TICK);
		RegulusGreedController.tickMagnet(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		// Dropping the cast first: a pre-fire abort must not let the fire tick run
		// on a toggle that is already off. cancel() clears whatever cast record is
		// live — mania's own record expires at castUntil, so only touch it while it
		// is still tracked or a different ability's windup would be aborted.
		if (RegulusCastState.isCasting(player, ID)) {
			RegulusCastState.cancel(player);
		}
		// Cooldown only when a magnet really started — an abort before tick 19 is free.
		boolean hadMagnet = RegulusGreedController.hasMagnet(player);
		RegulusGreedController.releaseAndFreeze(player);
		if (hadMagnet) {
			AbilityCooldowns.setCooldownTicks(player, ID, COOLDOWN_TICKS);
		}
	}

	private static LivingEntity findTarget(ServerPlayer player) {
		Vec3 eyes = player.getEyePosition(1.0f);
		Vec3 look = player.getViewVector(1.0f);
		Vec3 end = eyes.add(look.scale(RAY_RANGE));
		HitResult blockHit = player.serverLevel().clip(new ClipContext(eyes, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDistSq = blockHit.getType() == HitResult.Type.MISS
				? RAY_RANGE * RAY_RANGE
				: blockHit.getLocation().distanceToSqr(eyes);

		AABB box = player.getBoundingBox().expandTowards(look.scale(RAY_RANGE)).inflate(1.5);
		LivingEntity best = null;
		double bestDist = maxDistSq;
		for (Entity entity : player.serverLevel().getEntities(player, box,
				e -> e instanceof LivingEntity le && TargetFilters.hostileTo(player).test(le))) {
			Vec3 pos = entity.getBoundingBox().getCenter();
			Vec3 toEntity = pos.subtract(eyes);
			double along = toEntity.dot(look);
			if (along <= 0) continue;
			Vec3 closest = eyes.add(look.scale(along));
			double offsetSq = closest.distanceToSqr(pos);
			double radius = entity.getBoundingBox().getSize() * 0.6;
			if (offsetSq > radius * radius) continue;
			double d = pos.distanceToSqr(eyes);
			if (d < bestDist) {
				bestDist = d;
				best = (LivingEntity) entity;
			}
		}
		return best;
	}
}
