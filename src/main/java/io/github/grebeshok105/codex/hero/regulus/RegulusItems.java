package io.github.grebeshok105.codex.hero.regulus;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.regulus.item.EvangelionItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

/** Regulus's items: the transformation suit and the Evangelion book (starts the madness reading). */
public final class RegulusItems {
	public static final TransformationItem REGULUS_SUIT = ModContent.item(
			"regulus_suit",
			new TransformationItem(ModId.of("regulus"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.regulus_suit.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.regulus_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.regulus_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.regulus_suit.lore.untransform", ChatFormatting.GOLD))))
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
