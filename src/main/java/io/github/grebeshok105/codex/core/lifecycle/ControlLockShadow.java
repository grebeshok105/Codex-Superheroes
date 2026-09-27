package io.github.grebeshok105.codex.core.lifecycle;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent record of the flag values an entity had before {@link EntityControlLock} took
 * control of them. Live locks are non-persistent and die when the entity unloads or the
 * server stops; this shadow is what lets {@link EntityControlLock#reconcile} restore the
 * entity's own flags on the next load.
 */
public record ControlLockShadow(Map<ControlLockKind, Boolean> previousValues) {
	public static final ControlLockShadow EMPTY = new ControlLockShadow(Map.of());

	public static final Codec<ControlLockShadow> CODEC = Codec
			.unboundedMap(ControlLockKind.CODEC, Codec.BOOL)
			.xmap(ControlLockShadow::new, ControlLockShadow::previousValues);

	public ControlLockShadow {
		previousValues = Map.copyOf(previousValues);
	}

	public ControlLockShadow with(ControlLockKind kind, boolean previousValue) {
		Map<ControlLockKind, Boolean> previousValues = new HashMap<>(this.previousValues);
		previousValues.put(kind, previousValue);
		return new ControlLockShadow(previousValues);
	}

	public ControlLockShadow without(ControlLockKind kind) {
		if (!previousValues.containsKey(kind)) {
			return this;
		}
		Map<ControlLockKind, Boolean> previousValues = new HashMap<>(this.previousValues);
		previousValues.remove(kind);
		return new ControlLockShadow(previousValues);
	}

	public boolean isEmpty() {
		return previousValues.isEmpty();
	}
}
