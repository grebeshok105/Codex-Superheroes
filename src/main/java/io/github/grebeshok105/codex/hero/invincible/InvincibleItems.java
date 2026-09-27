package io.github.grebeshok105.codex.hero.invincible;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class InvincibleItems {
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.BLUE,
			List.of(new TransformationLore.Line("item.superheroes.invincible_suit.lore.line1", ChatFormatting.YELLOW),
					new TransformationLore.Line("item.superheroes.invincible_suit.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.invincible_suit.lore.usage", ChatFormatting.AQUA),
					new TransformationLore.Line("item.superheroes.invincible_suit.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem SUIT = ModContent.item("invincible_suit",
			new TransformationItem(InvincibleHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private InvincibleItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(SUIT);
	}
}
