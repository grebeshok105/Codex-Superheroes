package io.github.grebeshok105.codex.content.boss.homelander;

import io.github.grebeshok105.codex.content.boss.homelander.entity.HomelanderBossEntities;
import io.github.grebeshok105.codex.content.boss.homelander.item.VoughtSignalItem;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

public final class HomelanderBossItems {
	public static final VoughtSignalItem VOUGHT_SIGNAL = ModContent.item(
			"vought_signal",
			new VoughtSignalItem(new Item.Properties().stacksTo(4).rarity(Rarity.EPIC))
	);

	public static final SpawnEggItem HOMELANDER_BOSS_SPAWN_EGG = ModContent.item(
			"homelander_boss_spawn_egg",
			new SpawnEggItem(HomelanderBossEntities.HOMELANDER_BOSS, 0x2FB200, 0x6BD43A,
					new Item.Properties().rarity(Rarity.EPIC))
	);

	private HomelanderBossItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(VOUGHT_SIGNAL);
		// HOMELANDER_BOSS_SPAWN_EGG is admin-gated (ModItemGroups.ADMIN_ONLY_ITEMS) — never on the mod tab.
	}
}
