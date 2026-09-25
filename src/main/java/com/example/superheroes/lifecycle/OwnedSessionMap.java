package com.example.superheroes.lifecycle;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime state that belongs to an online player (the owner) and must never outlive them: entries are
 * dropped when the owner leaves, dies or clears their hero (per {@link ClearOn}) and all entries on server stop.
 * It only removes entries: side effects of a former clear() stay explicit lifecycle hooks. Replaces the
 * hand-maintained static maps plus clear/resetAll registrations. Iteration order is insertion order.
 */
public final class OwnedSessionMap<K, V> implements Iterable<Map.Entry<K, V>> {
	public enum ClearOn { LEAVE, DEATH, HERO_CLEAR }

	private record Slot<V>(UUID owner, V value) {
	}

	private final Map<K, Slot<V>> entries = new LinkedHashMap<>();
	private final Map<UUID, Set<K>> byOwner = new HashMap<>();

	private OwnedSessionMap() {
	}

	public static <K, V> OwnedSessionMap<K, V> create(LifecycleRegistrar lifecycle, Set<ClearOn> clearOn) {
		OwnedSessionMap<K, V> map = new OwnedSessionMap<>();
		EnumSet<ClearOn> policy = clearOn.isEmpty() ? EnumSet.noneOf(ClearOn.class) : EnumSet.copyOf(clearOn);
		if (policy.contains(ClearOn.LEAVE)) {
			lifecycle.onLeave(player -> map.removeOwnedBy(player.getUUID()));
		}
		if (policy.contains(ClearOn.DEATH)) {
			lifecycle.onDeath(player -> map.removeOwnedBy(player.getUUID()));
		}
		if (policy.contains(ClearOn.HERO_CLEAR)) {
			lifecycle.onHeroClear(player -> map.removeOwnedBy(player.getUUID()));
		}
		lifecycle.onServerStopped(server -> map.clear());
		return map;
	}

	/** Not bound to any lifecycle — for unit tests only. */
	static <K, V> OwnedSessionMap<K, V> unbound() {
		return new OwnedSessionMap<>();
	}

	public void put(K key, UUID owner, V value) {
		Objects.requireNonNull(value);
		remove(key);
		entries.put(key, new Slot<>(owner, value));
		byOwner.computeIfAbsent(owner, o -> new HashSet<>()).add(key);
	}

	@Nullable
	public V get(K key) {
		Slot<V> slot = entries.get(key);
		return slot == null ? null : slot.value();
	}

	@Nullable
	public V remove(K key) {
		Slot<V> slot = entries.remove(key);
		if (slot == null) {
			return null;
		}
		unindex(slot.owner(), key);
		return slot.value();
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	public int size() {
		return entries.size();
	}

	public void removeOwnedBy(UUID owner) {
		Set<K> keys = byOwner.remove(owner);
		if (keys != null) {
			keys.forEach(entries::remove);
		}
	}

	public void clear() {
		entries.clear();
		byOwner.clear();
	}

	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		Iterator<Map.Entry<K, Slot<V>>> raw = entries.entrySet().iterator();
		return new Iterator<>() {
			private Map.Entry<K, Slot<V>> current;

			@Override
			public boolean hasNext() {
				return raw.hasNext();
			}

			@Override
			public Map.Entry<K, V> next() {
				current = raw.next();
				return Map.entry(current.getKey(), current.getValue().value());
			}

			@Override
			public void remove() {
				raw.remove();
				unindex(current.getValue().owner(), current.getKey());
			}
		};
	}

	private void unindex(UUID owner, K key) {
		Set<K> keys = byOwner.get(owner);
		if (keys != null) {
			keys.remove(key);
			if (keys.isEmpty()) {
				byOwner.remove(owner);
			}
		}
	}
}
