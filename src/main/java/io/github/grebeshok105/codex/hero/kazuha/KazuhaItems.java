package io.github.grebeshok105.codex.hero.kazuha;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class KazuhaItems {
	// Same keys, colors and order as the lore row that lived in ModItems.
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.GREEN,
			List.of(new TransformationLore.Line("item.superheroes.kazuha_vision.lore.line1", ChatFormatting.GOLD),
					new TransformationLore.Line("item.superheroes.kazuha_vision.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.kazuha_vision.lore.usage", ChatFormatting.AQUA),
					new TransformationLore.Line("item.superheroes.kazuha_vision.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem KAZUHA_VISION = ModContent.item("kazuha_vision",
			new TransformationItem(KazuhaHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private KazuhaItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(KAZUHA_VISION);
	}
}
