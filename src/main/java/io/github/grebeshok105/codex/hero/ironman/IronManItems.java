package io.github.grebeshok105.codex.hero.ironman;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.ironman.item.IronManReactorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

/** Iron Man's items: the transformation suit and the Arc Reactor (the reactor charge source). */
public final class IronManItems {
	public static final TransformationItem IRON_MAN_SUIT = ModContent.item(
			"iron_man_suit",
			new TransformationItem(ModId.of("iron_man"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GOLD,
							List.of(new TransformationLore.Line("item.superheroes.iron_man_suit.lore.line1", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.iron_man_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.iron_man_suit.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.iron_man_suit.lore.untransform", ChatFormatting.RED))))
	);

	public static final IronManReactorItem IRON_MAN_REACTOR = ModContent.item(
			"iron_man_reactor",
			new IronManReactorItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE))
	);

	private IronManItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(IRON_MAN_SUIT);
		content.creativeTab(IRON_MAN_REACTOR);
	}
}
