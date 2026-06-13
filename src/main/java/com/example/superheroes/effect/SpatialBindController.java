package com.example.superheroes.effect;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Pandora's «Пространственная привязка» — the spatial ropes. When she binds her
 * trapped victims, each one is nailed in place by glowing red "ropes" anchored
 * into the sky and the ground around them, and is fully rooted (re-anchored to
 * the bind spot every tick, plus crippling slowness so even knockback can't
 * drag them out). The ropes are drawn purely with particle lines, so they
 * render on every client without any extra shader/model work.
 *
 * <p>A bound victim can later be finished with {@link SpaceCrushAbility}.
 */
public final class SpatialBindController {
	/** Glowing red rope colour (blood/greed theme). */
	private static final DustParticleOptions ROPE_DUST =
			new DustParticleOptions(new Vector3f(0.78f, 0.04f, 0.07f), 1.0f);
	/** Particles per rope segment line. */
	private static final int ROPE_SAMPLES = 14;
	/** How high above the victim the sky anchor sits. */
	private static final double SKY_ANCHOR_UP = 18.0;
	/** Horizontal reach of the four ground anchors. */
	private static final double GROUND_ANCHOR_OUT = 5.0;

	private static final Map<UUID, Bound> BOUND = new HashMap<>();

	private SpatialBindController() {
	}

	/** One rooted victim: who holds them, and the fixed anchor where they are pinned. */
	private static final class Bound {
		final UUID caster;
		final Vec3 anchor;

		Bound(UUID caster, Vec3 anchor) {
			this.caster = caster;
			this.anchor = anchor;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SpatialBindController::tick);
	}

	/** Binds (roots) the given victim to their current spot on behalf of the caster. */
	public static void bind(ServerPlayer caster, ServerPlayer victim) {
		BOUND.put(victim.getUUID(), new Bound(caster.getUUID(), victim.position()));
		// Visual/audio punch on the moment of binding.
		ServerLevel level = victim.serverLevel();
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, victim.getX(), victim.getY() + 1.0, victim.getZ(),
				40, 0.4, 0.9, 0.4, 0.01);
	}

	/** @return true if this victim is currently rope-bound. */
	public static boolean isBound(ServerPlayer victim) {
		return BOUND.containsKey(victim.getUUID());
	}

	/** Releases a single victim (e.g. when crushed, or when the House closes). */
	public static void release(UUID victimId) {
		BOUND.remove(victimId);
	}

	/** Releases every victim bound by the given caster (House closed / Pandora gone). */
	public static void releaseAllOf(UUID casterId) {
		BOUND.values().removeIf(b -> b.caster.equals(casterId));
	}

	private static void tick(MinecraftServer server) {
		if (BOUND.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Bound>> it = BOUND.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Bound> e = it.next();
			ServerPlayer victim = server.getPlayerList().getPlayer(e.getKey());
			if (victim == null || victim.isRemoved() || victim.isDeadOrDying() || victim.isSpectator()) {
				it.remove();
				continue;
			}
			// If the victim is no longer trapped in the House, the ropes fall away.
			if (!MirrorDimensionController.isTrapped(victim)) {
				it.remove();
				continue;
			}
			rootVictim(victim, e.getValue().anchor);
			drawRopes(victim, e.getValue().anchor);
		}
	}

	/** Pins the victim to the anchor and applies crippling movement debuffs. */
	private static void rootVictim(ServerPlayer victim, Vec3 anchor) {
		victim.setDeltaMovement(Vec3.ZERO);
		victim.hurtMarked = true;
		double dx = victim.getX() - anchor.x;
		double dz = victim.getZ() - anchor.z;
		// Snap back if they somehow drifted (knockback, pistons, etc.).
		if (dx * dx + dz * dz > 0.20 || Math.abs(victim.getY() - anchor.y) > 1.5) {
			victim.teleportTo(anchor.x, anchor.y, anchor.z);
		}
		victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 250, false, false, false));
		victim.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 200, false, false, false)); // negative jump = no jump
		victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 4, false, false, false));
	}

	/** Draws the glowing rope lines from the victim to one sky anchor and four ground anchors. */
	private static void drawRopes(ServerPlayer victim, Vec3 anchor) {
		ServerLevel level = victim.serverLevel();
		Vec3 chest = new Vec3(victim.getX(), victim.getY() + 1.1, victim.getZ());
		// Sky anchor — "nailed into the sky".
		ropeLine(level, chest, chest.add(0, SKY_ANCHOR_UP, 0));
		// Four ground/building anchors around the victim.
		double y = anchor.y;
		ropeLine(level, chest, new Vec3(anchor.x + GROUND_ANCHOR_OUT, y, anchor.z));
		ropeLine(level, chest, new Vec3(anchor.x - GROUND_ANCHOR_OUT, y, anchor.z));
		ropeLine(level, chest, new Vec3(anchor.x, y, anchor.z + GROUND_ANCHOR_OUT));
		ropeLine(level, chest, new Vec3(anchor.x, y, anchor.z - GROUND_ANCHOR_OUT));
	}

	private static void ropeLine(ServerLevel level, Vec3 from, Vec3 to) {
		for (int i = 0; i <= ROPE_SAMPLES; i++) {
			double t = i / (double) ROPE_SAMPLES;
			double x = from.x + (to.x - from.x) * t;
			double yy = from.y + (to.y - from.y) * t;
			double z = from.z + (to.z - from.z) * t;
			level.sendParticles(ROPE_DUST, x, yy, z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
