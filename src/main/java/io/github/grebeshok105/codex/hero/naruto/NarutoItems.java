package io.github.grebeshok105.codex.hero.naruto;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class NarutoItems {
	public static final TransformationItem NARUTO_HEADBAND = ModContent.item("naruto_headband",
			new TransformationItem(NarutoHero.ID, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.YELLOW,
							List.of(new TransformationLore.Line("item.superheroes.naruto_headband.lore.line1", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.naruto_headband.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.naruto_headband.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.naruto_headband.lore.untransform", ChatFormatting.RED)))));

	private NarutoItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(NARUTO_HEADBAND);
	}
}
