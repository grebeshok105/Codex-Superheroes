package com.example.superheroes.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Obstruction-aware destination clamp for ability teleports (audit B22).
 *
 * Walks the straight line from the entity's feet to the wanted destination and
 * returns the last point where the entity's bounding box is fully free of
 * blocks — i.e. "as far along the blink as the world allows". Unloaded chunks
 * count as blocked. Block collisions only: entity-vs-entity overlap does not
 * stop the walk, so "behind the target" blinks may still pass through mobs.
 */
public final class SafeTeleport {
	private SafeTeleport() {
	}

	/**
	 * @return the position along the from→dest segment where {@code entity} still
	 * fits; the entity's own position if the very first step is blocked.
	 */
	public static Vec3 clamp(ServerLevel level, Entity entity, Vec3 dest) {
		Vec3 from = entity.position();
		double dist = from.distanceTo(dest);
		if (dist < 1.0e-6) return dest;
		AABB localBox = entity.getDimensions(entity.getPose()).makeBoundingBox(Vec3.ZERO);
		double step = Math.max(0.2, Math.min(localBox.getXsize(), localBox.getZsize()) * 0.5);
		int steps = Math.max(1, (int) Math.ceil(dist / step));
		Vec3 last = from;
		for (int i = 1; i <= steps; i++) {
			Vec3 point = from.lerp(dest, (double) i / steps);
			if (!isFree(level, localBox, point)) break;
			last = point;
		}
		return last;
	}

	private static boolean isFree(ServerLevel level, AABB localBox, Vec3 pos) {
		Vec3 center = pos.add(0.0, localBox.getYsize() * 0.5, 0.0);
		if (!level.isLoaded(BlockPos.containing(center))) return false;
		return level.noCollision(localBox.move(pos));
	}
}
