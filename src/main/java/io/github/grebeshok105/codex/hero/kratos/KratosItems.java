package io.github.grebeshok105.codex.hero.kratos;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class KratosItems {
	public static final TransformationItem BLADE_OF_CHAOS = ModContent.item(
			"blade_of_chaos",
			new TransformationItem(KratosHero.ID, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.RED,
							List.of(new TransformationLore.Line("item.superheroes.blade_of_chaos.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.blade_of_chaos.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.blade_of_chaos.lore.usage", ChatFormatting.GOLD),
									new TransformationLore.Line("item.superheroes.blade_of_chaos.lore.untransform", ChatFormatting.RED))))
	);

	private KratosItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(BLADE_OF_CHAOS);
	}
}
