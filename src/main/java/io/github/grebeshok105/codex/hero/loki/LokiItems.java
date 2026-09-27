package io.github.grebeshok105.codex.hero.loki;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class LokiItems {
	public static final TransformationItem LOKI_SCEPTER = ModContent.item("loki_scepter",
			new TransformationItem(LokiHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.GREEN,
							List.of(new TransformationLore.Line("item.superheroes.loki_scepter.lore.line1", ChatFormatting.GREEN),
									new TransformationLore.Line("item.superheroes.loki_scepter.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.loki_scepter.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.loki_scepter.lore.untransform", ChatFormatting.RED)))));

	private LokiItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(LOKI_SCEPTER);
	}
}
