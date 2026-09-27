package io.github.grebeshok105.codex.hero.scaramouche;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class ScaramoucheItems {
	// Same keys, colors and order as the lore row that lived in ModItems.
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.DARK_AQUA,
			List.of(new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.line1", ChatFormatting.AQUA),
					new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.usage", ChatFormatting.AQUA),
					new TransformationLore.Line("item.superheroes.scaramouche_vision.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem SCARAMOUCHE_VISION = ModContent.item("scaramouche_vision",
			new TransformationItem(ScaramoucheHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private ScaramoucheItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(SCARAMOUCHE_VISION);
	}
}
