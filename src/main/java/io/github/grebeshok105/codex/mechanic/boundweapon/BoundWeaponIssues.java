package io.github.grebeshok105.codex.mechanic.boundweapon;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/** Per-player record of the currently valid issue of each bound weapon. Deliberately not persistent. */
public record BoundWeaponIssues(Map<Item, Long> current) {
	public static final BoundWeaponIssues EMPTY = new BoundWeaponIssues(Map.of());

	public BoundWeaponIssues {
		current = Map.copyOf(current);
	}

	public OptionalLong issueOf(Item item) {
		Long issue = current.get(item);
		return issue == null ? OptionalLong.empty() : OptionalLong.of(issue);
	}

	public BoundWeaponIssues with(Item item, long issue) {
		Map<Item, Long> map = new HashMap<>(current);
		map.put(item, issue);
		return new BoundWeaponIssues(map);
	}

	public BoundWeaponIssues without(Item item) {
		if (!current.containsKey(item)) {
			return this;
		}
		Map<Item, Long> map = new HashMap<>(current);
		map.remove(item);
		return new BoundWeaponIssues(map);
	}
}
