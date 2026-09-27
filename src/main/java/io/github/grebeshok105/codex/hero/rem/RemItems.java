package io.github.grebeshok105.codex.hero.rem;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.rem.item.RemMorningStarItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class RemItems {
	public static final TransformationItem REM_ONI_HORN = ModContent.item(
			"rem_oni_horn",
			new TransformationItem(RemHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.BLUE,
							List.of(new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.line1", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.usage", ChatFormatting.AQUA),
									new TransformationLore.Line("item.superheroes.rem_oni_horn.lore.untransform", ChatFormatting.RED))))
	);

	public static final RemMorningStarItem REM_MORNING_STAR = ModContent.item(
			"rem_morning_star",
			new RemMorningStarItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private RemItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(REM_ONI_HORN);
	}
}
