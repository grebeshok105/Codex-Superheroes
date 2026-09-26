package io.github.grebeshok105.codex.lifecycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reverse index kept on the lock owner (always a player today): which entities they hold
 * locks on, and for which kinds. Lets {@link EntityControlLock#releaseOwnedBy} free every
 * victim when the owner leaves, dies, or changes hero. Deliberately not persistent.
 */
public record HeldLocks(Map<UUID, Set<ControlLockKind>> held) {
	public static final HeldLocks EMPTY = new HeldLocks(Map.of());

	public HeldLocks {
		Map<UUID, Set<ControlLockKind>> copy = new HashMap<>();
		held.forEach((victim, kinds) -> copy.put(victim, Set.copyOf(kinds)));
		held = copy;
	}

	public HeldLocks with(UUID victim, ControlLockKind kind) {
		Map<UUID, Set<ControlLockKind>> held = new HashMap<>(this.held);
		Set<ControlLockKind> kinds = new HashSet<>(held.getOrDefault(victim, Set.of()));
		kinds.add(kind);
		held.put(victim, kinds);
		return new HeldLocks(held);
	}

	public HeldLocks without(UUID victim, ControlLockKind kind) {
		Set<ControlLockKind> kinds = held.get(victim);
		if (kinds == null || !kinds.contains(kind)) {
			return this;
		}
		Map<UUID, Set<ControlLockKind>> held = new HashMap<>(this.held);
		Set<ControlLockKind> remaining = new HashSet<>(kinds);
		remaining.remove(kind);
		if (remaining.isEmpty()) {
			held.remove(victim);
		} else {
			held.put(victim, remaining);
		}
		return new HeldLocks(held);
	}

	public boolean isEmpty() {
		return held.isEmpty();
	}
}
