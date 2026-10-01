package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.net.FxBroadcast;
import io.github.grebeshok105.codex.core.net.ScreenShakeS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.hero.regulus.sound.RegulusSounds;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import io.github.grebeshok105.codex.mechanic.world.WorldDestructionPolicy;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The debris kick's fire tick — a pellet-rule shotgun fan instead of the old flat
 * cone. Nine rays inside a 35-degree cone at {@value #RANGE} blocks: one through
 * the crosshair, eight evenly spaced on the rim at ±{@value #CONE_HALF_ANGLE_DEG}°.
 * Each ray independently re-runs a COLLIDER block raycast through the walls
 * it just broke (at most {@value #MAX_BREAKS_PER_RAY} per ray, dead stop on the
 * first block {@link WorldDestructionPolicy} refuses), then hits every hostile
 * whose AABB the surviving segment crosses — no dedupe, so a wide target standing
 * on several rays eats several pellets. Each pellet lands
 * {@code {@value #PELLET_DAMAGE} * RegulusHearts.damageScale} as
 * {@code regulus_debris} damage.
 */
public final class DebrisKickController {
	public static final double RANGE = 16.0;
	public static final float PELLET_DAMAGE = 7.0f;

	private static final int RIM_RAYS = 8;
	private static final double CONE_HALF_ANGLE_DEG = 17.5;
	private static final double CONE_TAN = Math.tan(Math.toRadians(CONE_HALF_ANGLE_DEG));
	private static final int MAX_BREAKS_PER_RAY = 3;
	private static final double KNOCKBACK = 1.5;
	private static final double KNOCKBACK_UP = 0.45;
	private static final double SHAKE_RADIUS = 24.0;

	private DebrisKickController() {
	}

	public static void fire(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();
		float damage = PELLET_DAMAGE * RegulusHearts.damageScale(player);

		for (Vec3 dir : fan(forward)) {
			Vec3 rayEnd = traceRay(level, player, eye, dir);
			AABB sweep = new AABB(eye, rayEnd).inflate(1.0);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, sweep,
					TargetFilters.hostileTo(player))) {
				if (victim.getBoundingBox().clip(eye, rayEnd).isEmpty()) {
					continue;
				}
				victim.hurt(RegulusDamageTypes.debris(level, player), damage);
				victim.push(dir.x * KNOCKBACK, dir.y * KNOCKBACK + KNOCKBACK_UP, dir.z * KNOCKBACK);
				victim.hurtMarked = true;
			}
		}

		level.playSound(null, player, RegulusSounds.DEBRIS_ROAR, SoundSource.PLAYERS, 1.6f, 0.85f);
		level.playSound(null, eye.x, eye.y, eye.z,
				RegulusSounds.DEBRIS_IMPACT, SoundSource.PLAYERS, 2.0f, 0.8f);
		FxBroadcast.aroundAudience(level, eye, SHAKE_RADIUS).forEach(near ->
				ServerPlayNetworking.send(near, new ScreenShakeS2CPayload(1.2f, 12)));
		VfxFx.event(player, RegulusVfxIds.DEBRIS_IMPACT,
				eye, eye.add(forward.scale(RANGE)), 1f);
	}

	/** Center ray plus the {@value #RIM_RAYS}-ray rim evenly spaced at the cone edge. */
	private static List<Vec3> fan(Vec3 forward) {
		Vec3 up = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 u = forward.cross(up).normalize();
		Vec3 v = forward.cross(u).normalize();
		List<Vec3> rays = new ArrayList<>(RIM_RAYS + 1);
		rays.add(forward);
		for (int i = 0; i < RIM_RAYS; i++) {
			double azimuth = Math.toRadians(360.0 * i / RIM_RAYS);
			rays.add(forward
					.add(u.scale(Math.cos(azimuth) * CONE_TAN))
					.add(v.scale(Math.sin(azimuth) * CONE_TAN))
					.normalize());
		}
		return rays;
	}

	/**
	 * One ray's march: clip, break the hit block when the budget allows, resume from
	 * the freshly opened face. Returns the point where the ray finally dies — on the
	 * first unbreakable block, on the fourth breakable one, or at full range.
	 */
	private static Vec3 traceRay(ServerLevel level, ServerPlayer caster, Vec3 eye, Vec3 dir) {
		Vec3 from = eye;
		Vec3 end = eye.add(dir.scale(RANGE));
		for (int breaks = 0; ; ) {
			BlockHitResult hit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, caster));
			if (hit.getType() != HitResult.Type.BLOCK) {
				return end;
			}
			if (breaks >= MAX_BREAKS_PER_RAY) {
				return hit.getLocation();
			}
			BlockPos pos = hit.getBlockPos();
			if (!WorldDestructionPolicy.tryBreak(level, pos, true, caster)) {
				return hit.getLocation();
			}
			breaks++;
			// Step a hair off the face we just opened so the next clip marches forward.
			from = hit.getLocation().add(dir.scale(1.0e-3));
		}
	}
}
