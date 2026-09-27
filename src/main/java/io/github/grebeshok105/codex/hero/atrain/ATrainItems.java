package io.github.grebeshok105.codex.hero.atrain;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class ATrainItems {
	// Same keys, colors and order as the lore row that lived in ModItems.
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.BLUE,
			List.of(new TransformationLore.Line("item.superheroes.a_train_suit.lore.line1", ChatFormatting.RED),
					new TransformationLore.Line("item.superheroes.a_train_suit.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.a_train_suit.lore.usage", ChatFormatting.AQUA),
					new TransformationLore.Line("item.superheroes.a_train_suit.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem A_TRAIN_SUIT = ModContent.item("a_train_suit",
			new TransformationItem(ATrainHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private ATrainItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(A_TRAIN_SUIT);
	}
}
