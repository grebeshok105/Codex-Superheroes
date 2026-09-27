package io.github.grebeshok105.codex.hero.captainamerica;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.captainamerica.item.CaptainAmericaSuitItem;
import io.github.grebeshok105.codex.hero.captainamerica.item.VibraniumShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class CaptainAmericaItems {
	public static final CaptainAmericaSuitItem CAPTAIN_AMERICA_SUIT = ModContent.item(
			"captain_america_suit",
			new CaptainAmericaSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final VibraniumShieldItem VIBRANIUM_SHIELD = ModContent.item(
			"vibranium_shield",
			new VibraniumShieldItem(new Item.Properties().stacksTo(1).durability(2000).rarity(Rarity.EPIC))
	);

	private CaptainAmericaItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(CAPTAIN_AMERICA_SUIT);
		content.creativeTab(VIBRANIUM_SHIELD);
	}
}
