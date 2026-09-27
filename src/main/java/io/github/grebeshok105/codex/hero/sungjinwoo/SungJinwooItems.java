package io.github.grebeshok105.codex.hero.sungjinwoo;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.sungjinwoo.item.ShadowMonarchsCloakItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Sung Jin-Woo's items: the transformation cloak. */
public final class SungJinwooItems {
	public static final ShadowMonarchsCloakItem SHADOW_MONARCHS_CLOAK = ModContent.item(
			"shadow_monarchs_cloak",
			new ShadowMonarchsCloakItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private SungJinwooItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(SHADOW_MONARCHS_CLOAK);
	}
}
