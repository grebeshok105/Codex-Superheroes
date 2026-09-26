package io.github.grebeshok105.codex.lifecycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Live control locks held on an entity. Deliberately not persistent: locks die with the
 * entity instance, while {@link ControlLockShadow} records what must be undone on reload.
 */
public record ControlLockState(Map<ControlLockKind, Entry> locks) {
	public static final ControlLockState EMPTY = new ControlLockState(Map.of());

	public ControlLockState {
		locks = Map.copyOf(locks);
	}

	public Entry get(ControlLockKind kind) {
		return locks.get(kind);
	}

	public ControlLockState with(ControlLockKind kind, Entry entry) {
		Map<ControlLockKind, Entry> locks = new HashMap<>(this.locks);
		locks.put(kind, entry);
		return new ControlLockState(locks);
	}

	public ControlLockState without(ControlLockKind kind) {
		if (!locks.containsKey(kind)) {
			return this;
		}
		Map<ControlLockKind, Entry> locks = new HashMap<>(this.locks);
		locks.remove(kind);
		return new ControlLockState(locks);
	}

	public boolean isEmpty() {
		return locks.isEmpty();
	}

	public record Entry(boolean previousValue, Set<UUID> owners) {
		public Entry {
			owners = Set.copyOf(owners);
		}

		public Entry withOwner(UUID owner) {
			Set<UUID> owners = new HashSet<>(this.owners);
			owners.add(owner);
			return new Entry(previousValue, owners);
		}

		public Entry withoutOwner(UUID owner) {
			Set<UUID> owners = new HashSet<>(this.owners);
			owners.remove(owner);
			return new Entry(previousValue, owners);
		}
	}
}
