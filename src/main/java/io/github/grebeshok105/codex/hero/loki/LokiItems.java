package io.github.grebeshok105.codex.hero.loki;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.loki.item.LokiScepterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class LokiItems {
	public static final LokiScepterItem LOKI_SCEPTER = ModContent.item("loki_scepter",
			new LokiScepterItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

	private LokiItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(LOKI_SCEPTER);
	}
}
