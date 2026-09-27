package io.github.grebeshok105.codex.hero.reinhard;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.reinhard.item.RoyalIcicleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

/** Reinhard's items: the transformation suit and Reid (the bound sword, issued by the draw ceremony). */
public final class ReinhardItems {
	// Same keys, colors and order as the lore moved out of ModItems.
	public static final TransformationItem REINHARD_SUIT = ModContent.item(
			"reinhard_suit",
			new TransformationItem(ReinhardHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.reinhard_suit.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.reinhard_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.reinhard_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.reinhard_suit.lore.untransform", ChatFormatting.RED))))
	);

	// Not in the creative tab: the bound sword is issued by the draw ceremony, not given freely.
	public static final RoyalIcicleItem ROYAL_ICICLE = ModContent.item(
			"royal_icicle",
			new RoyalIcicleItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private ReinhardItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(REINHARD_SUIT);
	}
}
