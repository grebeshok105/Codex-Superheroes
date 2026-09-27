package io.github.grebeshok105.codex.hero.regulus;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.regulus.item.EvangelionItem;
import io.github.grebeshok105.codex.hero.regulus.item.RegulusSuitItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Regulus's items: the transformation suit and the Evangelion book (starts the madness reading). */
public final class RegulusItems {
	public static final RegulusSuitItem REGULUS_SUIT = ModContent.item(
			"regulus_suit",
			new RegulusSuitItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final EvangelionItem EVANGELION = ModContent.item(
			"evangelion",
			new EvangelionItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private RegulusItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(REGULUS_SUIT);
		content.creativeTab(EVANGELION);
	}
}
