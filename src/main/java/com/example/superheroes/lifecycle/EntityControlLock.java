package com.example.superheroes.lifecycle;

import com.example.superheroes.attachment.ModAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.UUID;

/**
 * Shared mechanism for temporary control of the entity flags that persist to NBT
 * (NoAI, NoGravity, noPhysics, invulnerable). Controllers {@link #acquire} a lock instead of
 * flipping the flag directly: the first owner records the flag's previous value, subsequent
 * owners only bump the refcount, and the flag is restored when the last owner releases.
 *
 * <p>Locks are released on player {@link PlayerLifecycle#onLeave leave}, death, and hero
 * change ({@code HeroTransformService.clearHeroRuntimeState}). If a locked entity unloads
 * while still locked, the flag and the {@link ControlLockShadow} persist to NBT, and
 * {@link #reconcile} on the next {@code ENTITY_LOAD} restores the previous value.
 */
public final class EntityControlLock {
	private EntityControlLock() {
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> reconcile(entity));
	}

	/** Take a lock on {@code kind} for {@code owner}, applying the flag on first acquire. */
	public static void acquire(Entity victim, ControlLockKind kind, ServerPlayer owner) {
		if (!kind.appliesTo(victim)) {
			return;
		}
		ControlLockState state = attachedOrEmpty(victim);
		ControlLockState.Entry entry = state.get(kind);
		if (entry == null) {
			entry = new ControlLockState.Entry(getFlag(victim, kind), java.util.Set.of());
			writeShadow(victim, kind, entry.previousValue());
		}
		victim.setAttached(ModAttachments.CONTROL_LOCKS,
				state.with(kind, entry.withOwner(owner.getUUID())));
		setFlag(victim, kind, true);
		trackOnOwner(owner, victim.getUUID(), kind);
	}

	/**
	 * Drop {@code ownerId}'s reference on {@code kind}; restores the victim's own flag value
	 * when the last owner releases.
	 */
	public static void release(Entity victim, ControlLockKind kind, UUID ownerId) {
		ControlLockState state = victim.getAttached(ModAttachments.CONTROL_LOCKS);
		if (state == null) {
			return;
		}
		ControlLockState.Entry entry = state.get(kind);
		if (entry == null || !entry.owners().contains(ownerId)) {
			return;
		}
		entry = entry.withoutOwner(ownerId);
		if (entry.owners().isEmpty()) {
			victim.setAttached(ModAttachments.CONTROL_LOCKS, state.without(kind));
			setFlag(victim, kind, entry.previousValue());
			clearShadow(victim, kind);
		} else {
			victim.setAttached(ModAttachments.CONTROL_LOCKS, state.with(kind, entry));
		}
		untrackOnOwner(victim, ownerId, kind);
	}

	/** Restore every locked flag on the victim, regardless of owner. */
	public static void releaseAll(Entity victim) {
		ControlLockState state = victim.getAttached(ModAttachments.CONTROL_LOCKS);
		if (state == null) {
			return;
		}
		for (Map.Entry<ControlLockKind, ControlLockState.Entry> lock : state.locks().entrySet()) {
			setFlag(victim, lock.getKey(), lock.getValue().previousValue());
			clearShadow(victim, lock.getKey());
			lock.getValue().owners().forEach(ownerId -> untrackOnOwner(victim, ownerId, lock.getKey()));
		}
		victim.setAttached(ModAttachments.CONTROL_LOCKS, null);
	}

	/** Release every lock {@code owner} holds, across all loaded levels. */
	public static void releaseOwnedBy(ServerPlayer owner) {
		HeldLocks held = owner.getAttached(ModAttachments.HELD_LOCKS);
		if (held == null || held.isEmpty()) {
			return;
		}
		MinecraftServer server = owner.server;
		held.held().forEach((victimId, kinds) -> {
			Entity victim = findEntity(server, victimId);
			if (victim == null) {
				return;
			}
			for (ControlLockKind kind : kinds) {
				release(victim, kind, owner.getUUID());
			}
		});
		owner.setAttached(ModAttachments.HELD_LOCKS, null);
	}

	public static boolean isLocked(Entity victim, ControlLockKind kind) {
		ControlLockState state = victim.getAttached(ModAttachments.CONTROL_LOCKS);
		return state != null && state.get(kind) != null;
	}

	/**
	 * ENTITY_LOAD handler. Live locks are non-persistent, so an entity saved while locked
	 * comes back with the flag still set and only the shadow to say what its own value was;
	 * restore it unless a live lock (re-acquired during load) already owns the flag.
	 */
	public static void reconcile(Entity entity) {
		ControlLockShadow shadow = entity.getAttached(ModAttachments.CONTROL_LOCK_SHADOW);
		if (shadow == null || shadow.isEmpty()) {
			return;
		}
		ControlLockState live = entity.getAttached(ModAttachments.CONTROL_LOCKS);
		for (Map.Entry<ControlLockKind, Boolean> entry : shadow.previousValues().entrySet()) {
			ControlLockKind kind = entry.getKey();
			if (live != null && live.get(kind) != null) {
				continue;
			}
			if (kind.appliesTo(entity)) {
				setFlag(entity, kind, entry.getValue());
			}
			shadow = shadow.without(kind);
		}
		entity.setAttached(ModAttachments.CONTROL_LOCK_SHADOW, shadow.isEmpty() ? null : shadow);
	}

	private static ControlLockState attachedOrEmpty(Entity victim) {
		ControlLockState state = victim.getAttached(ModAttachments.CONTROL_LOCKS);
		return state == null ? ControlLockState.EMPTY : state;
	}

	private static Entity findEntity(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(id);
			if (entity != null) {
				return entity;
			}
		}
		return null;
	}

	private static void trackOnOwner(ServerPlayer owner, UUID victimId, ControlLockKind kind) {
		HeldLocks held = owner.getAttached(ModAttachments.HELD_LOCKS);
		owner.setAttached(ModAttachments.HELD_LOCKS,
				(held == null ? HeldLocks.EMPTY : held).with(victimId, kind));
	}

	/**
	 * Best-effort removal of a held-lock entry from the owner's index. Owners are players;
	 * an offline owner's stale entry is dropped the next time {@link #releaseOwnedBy} runs
	 * for them, and an unloaded victim is covered by its shadow on next load.
	 */
	private static void untrackOnOwner(Entity victim, UUID ownerId, ControlLockKind kind) {
		MinecraftServer server = victim.level().getServer();
		if (server == null) {
			return;
		}
		ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
		if (owner == null) {
			return;
		}
		HeldLocks held = owner.getAttached(ModAttachments.HELD_LOCKS);
		if (held == null) {
			return;
		}
		held = held.without(victim.getUUID(), kind);
		owner.setAttached(ModAttachments.HELD_LOCKS, held.isEmpty() ? null : held);
	}

	private static void writeShadow(Entity victim, ControlLockKind kind, boolean previousValue) {
		ControlLockShadow shadow = victim.getAttached(ModAttachments.CONTROL_LOCK_SHADOW);
		victim.setAttached(ModAttachments.CONTROL_LOCK_SHADOW,
				(shadow == null ? ControlLockShadow.EMPTY : shadow).with(kind, previousValue));
	}

	private static void clearShadow(Entity victim, ControlLockKind kind) {
		ControlLockShadow shadow = victim.getAttached(ModAttachments.CONTROL_LOCK_SHADOW);
		if (shadow == null) {
			return;
		}
		shadow = shadow.without(kind);
		victim.setAttached(ModAttachments.CONTROL_LOCK_SHADOW, shadow.isEmpty() ? null : shadow);
	}

	private static boolean getFlag(Entity entity, ControlLockKind kind) {
		return switch (kind) {
			case NO_AI -> entity instanceof Mob mob && mob.isNoAi();
			case NO_GRAVITY -> entity.isNoGravity();
			case NO_PHYSICS -> entity.noPhysics;
			case INVULNERABLE -> entity.isInvulnerable();
		};
	}

	private static void setFlag(Entity entity, ControlLockKind kind, boolean value) {
		switch (kind) {
			case NO_AI -> {
				if (entity instanceof Mob mob) {
					mob.setNoAi(value);
				}
			}
			case NO_GRAVITY -> entity.setNoGravity(value);
			case NO_PHYSICS -> entity.noPhysics = value;
			case INVULNERABLE -> entity.setInvulnerable(value);
		}
	}
}
