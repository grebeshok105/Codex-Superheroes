package io.github.grebeshok105.codex.content.horde;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;

public final class HordeItems {
	public static final HordeCrystalItem HORDE_CRYSTAL = ModContent.item("horde_crystal", new HordeCrystalItem());

	private HordeItems() {
	}

	static void register(ContentRegistrar content) {
		// HORDE_CRYSTAL is admin-gated (ModItemGroups.ADMIN_ONLY_ITEMS) — never on the mod tab.
	}
}
