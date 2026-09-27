package io.github.grebeshok105.codex.content.horde.entity;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Mob-death hook for the horde: entities live a package below {@code HordeManager}, so the
 * manager registers its bookkeeping here instead of entities calling back up.
 */
public final class HordeDeaths {
	private static BiConsumer<UUID, UUID> onMobDied = (hordeId, entityId) -> {
	};

	private HordeDeaths() {
	}

	public static void onMobDied(BiConsumer<UUID, UUID> listener) {
		onMobDied = listener;
	}

	public static void fireMobDied(UUID hordeId, UUID entityId) {
		onMobDied.accept(hordeId, entityId);
	}
}
