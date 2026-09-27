package io.github.grebeshok105.codex.hero.goku;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class GokuItems {
	private static final TransformationLore GI_LORE = new TransformationLore(ChatFormatting.GOLD,
			List.of(new TransformationLore.Line("item.superheroes.goku_gi.lore.line1", ChatFormatting.GOLD),
					new TransformationLore.Line("item.superheroes.goku_gi.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.goku_gi.lore.usage", ChatFormatting.YELLOW),
					new TransformationLore.Line("item.superheroes.goku_gi.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem GOKU_GI = ModContent.item("goku_gi",
			new TransformationItem(GokuHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					GI_LORE));

	private GokuItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(GOKU_GI);
	}
}
