package io.github.grebeshok105.codex.hero.captainamerica;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.captainamerica.item.VibraniumShieldItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class CaptainAmericaItems {
	public static final TransformationItem CAPTAIN_AMERICA_SUIT = ModContent.item(
			"captain_america_suit",
			new TransformationItem(CaptainAmericaHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.BLUE,
							List.of(new TransformationLore.Line("item.superheroes.captain_america_suit.lore.line1", ChatFormatting.BLUE),
									new TransformationLore.Line("item.superheroes.captain_america_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.captain_america_suit.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.captain_america_suit.lore.untransform", ChatFormatting.RED))))
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
