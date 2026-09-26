package io.github.grebeshok105.codex.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnedSessionMapTest {
	private final UUID alice = UUID.randomUUID();
	private final UUID bob = UUID.randomUUID();

	@Test
	void removingAnOwnerDropsEveryEntryItOwns() {
		OwnedSessionMap<String, Integer> map = OwnedSessionMap.unbound();
		map.put("pull-1", alice, 1);
		map.put("pull-2", alice, 2);
		map.put("pull-3", bob, 3);

		map.removeOwnedBy(alice);

		assertNull(map.get("pull-1"));
		assertNull(map.get("pull-2"));
		assertEquals(3, map.get("pull-3"));
	}

	@Test
	void reputtingAKeyMovesItToTheNewOwner() {
		OwnedSessionMap<String, Integer> map = OwnedSessionMap.unbound();
		map.put("target", alice, 1);
		map.put("target", bob, 2);

		map.removeOwnedBy(alice);

		assertEquals(2, map.get("target"));
	}

	@Test
	void iteratorRemovalKeepsTheOwnerIndexConsistent() {
		OwnedSessionMap<String, Integer> map = OwnedSessionMap.unbound();
		map.put("a", alice, 1);
		for (Iterator<Map.Entry<String, Integer>> it = map.iterator(); it.hasNext(); ) {
			it.next();
			it.remove();
		}
		map.put("a", bob, 2);
		map.removeOwnedBy(alice);
		assertTrue(map.containsKey("a"));
		map.clear();
		assertFalse(map.containsKey("a"));
		assertEquals(0, map.size());
	}
}
