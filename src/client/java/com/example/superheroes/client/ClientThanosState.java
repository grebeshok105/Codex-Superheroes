package com.example.superheroes.client;

import com.example.superheroes.item.infinity.InfinityStoneType;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Камни бесконечности игроков-Таносов на клиенте (битовые маски по UUID).
 * Локальные хелперы ({@link #hasStone}, {@link #count}, {@link #hasAllStones})
 * читают маску собственного игрока; {@link #maskFor} — любого игрока (для
 * скина чужих Таносов).
 */
public final class ClientThanosState {
	private static final int FULL_MASK = (1 << InfinityStoneType.values().length) - 1;
	private static final Map<UUID, Integer> MASKS = new HashMap<>();

	private ClientThanosState() {
	}

	public static synchronized void update(UUID playerId, int bitmask) {
		if (bitmask == 0) {
			MASKS.remove(playerId);
		} else {
			MASKS.put(playerId, bitmask);
		}
	}

	public static synchronized int maskFor(UUID playerId) {
		Integer mask = MASKS.get(playerId);
		return mask == null ? 0 : mask;
	}

	public static int mask() {
		return maskFor(localId());
	}

	public static boolean hasStone(InfinityStoneType type) {
		return (mask() & (1 << type.ordinal())) != 0;
	}

	public static int count() {
		return Integer.bitCount(mask());
	}

	public static boolean hasAllStones() {
		return (mask() & FULL_MASK) == FULL_MASK;
	}

	public static synchronized void clear() {
		MASKS.clear();
	}

	private static UUID localId() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null ? null : mc.player.getUUID();
	}
}
