package io.github.grebeshok105.codex.hero.sungjinwoo;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

/** Sung Jin-Woo's items: the transformation cloak. */
public final class SungJinwooItems {
	public static final TransformationItem SHADOW_MONARCHS_CLOAK = ModContent.item(
			"shadow_monarchs_cloak",
			new TransformationItem(ModId.of("sung_jinwoo"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.LIGHT_PURPLE,
							List.of(new TransformationLore.Line("item.superheroes.shadow_monarchs_cloak.lore.line1", ChatFormatting.LIGHT_PURPLE),
									new TransformationLore.Line("item.superheroes.shadow_monarchs_cloak.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.shadow_monarchs_cloak.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.shadow_monarchs_cloak.lore.untransform", ChatFormatting.LIGHT_PURPLE))))
	);

	private SungJinwooItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(SHADOW_MONARCHS_CLOAK);
	}
}
