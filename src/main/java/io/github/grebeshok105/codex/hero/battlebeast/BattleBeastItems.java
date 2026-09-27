package io.github.grebeshok105.codex.hero.battlebeast;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class BattleBeastItems {
	// Same keys, colors and order as the lore row that lived in ModItems.
	private static final TransformationLore LORE = new TransformationLore(ChatFormatting.DARK_RED,
			List.of(new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.line1", ChatFormatting.RED),
					new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.usage", ChatFormatting.GOLD),
					new TransformationLore.Line("item.superheroes.battle_beast_medallion.lore.untransform", ChatFormatting.RED)));

	public static final TransformationItem BATTLE_BEAST_MEDALLION = ModContent.item("battle_beast_medallion",
			new TransformationItem(BattleBeastHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC), LORE));

	private BattleBeastItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(BATTLE_BEAST_MEDALLION);
	}
}
