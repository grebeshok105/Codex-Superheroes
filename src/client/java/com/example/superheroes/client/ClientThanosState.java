package com.example.superheroes.client;

import com.example.superheroes.item.infinity.InfinityStoneType;

import java.util.EnumSet;

public final class ClientThanosState {
	private static final EnumSet<InfinityStoneType> STONES = EnumSet.noneOf(InfinityStoneType.class);

	private ClientThanosState() {
	}

	public static synchronized void updateFromBitmask(int bitmask) {
		STONES.clear();
		for (InfinityStoneType t : InfinityStoneType.values()) {
			if ((bitmask & (1 << t.ordinal())) != 0) {
				STONES.add(t);
			}
		}
	}

	public static synchronized boolean hasStone(InfinityStoneType type) {
		return STONES.contains(type);
	}

	public static synchronized int count() {
		return STONES.size();
	}

	public static synchronized boolean hasAllStones() {
		return STONES.size() >= InfinityStoneType.values().length;
	}

	public static synchronized void clear() {
		STONES.clear();
	}
}
