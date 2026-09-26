package io.github.grebeshok105.codex.hero.scorpion;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class ScorpionItems {
	// Same keys, colors and order as the lore B3 moved out of ScorpionKunaiItem.
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.GOLD,
			List.of(new TransformationLore.Line("item.superheroes.scorpion_kunai.lore.line1", ChatFormatting.GOLD),
					new TransformationLore.Line("item.superheroes.scorpion_kunai.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.scorpion_kunai.lore.usage", ChatFormatting.RED),
					new TransformationLore.Line("item.superheroes.scorpion_kunai.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem KUNAI = ModContent.item("scorpion_kunai",
			new TransformationItem(ScorpionHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private ScorpionItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(KUNAI);
	}
}
