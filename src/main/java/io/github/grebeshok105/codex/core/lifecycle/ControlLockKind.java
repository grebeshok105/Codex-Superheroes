package io.github.grebeshok105.codex.core.lifecycle;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * A single entity flag that an ability can borrow through {@link EntityControlLock}.
 * Each kind maps to one NBT-backed flag on the entity.
 */
public enum ControlLockKind {
	NO_AI,
	NO_GRAVITY,
	NO_PHYSICS,
	INVULNERABLE;

	public static final Codec<ControlLockKind> CODEC = Codec.STRING.xmap(ControlLockKind::valueOf, ControlLockKind::name);

	/** Whether this flag can be applied to the given entity at all (NoAI only exists on mobs). */
	public boolean appliesTo(Entity entity) {
		return this != NO_AI || entity instanceof Mob;
	}
}
