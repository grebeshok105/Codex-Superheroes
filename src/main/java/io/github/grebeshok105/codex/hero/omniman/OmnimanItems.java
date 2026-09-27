package io.github.grebeshok105.codex.hero.omniman;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class OmnimanItems {
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.DARK_RED,
			List.of(new TransformationLore.Line("item.superheroes.omniman_suit.lore.line1", ChatFormatting.RED),
					new TransformationLore.Line("item.superheroes.omniman_suit.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.omniman_suit.lore.usage", ChatFormatting.GOLD),
					new TransformationLore.Line("item.superheroes.omniman_suit.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem SUIT = ModContent.item("omniman_suit",
			new TransformationItem(OmnimanHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private OmnimanItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(SUIT);
	}
}
