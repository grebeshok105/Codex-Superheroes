package io.github.grebeshok105.codex.hero.raiden;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.raiden.item.MusouNoHitotachiItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

/** Raiden's items: the transformation suit and Musou no Hitotachi (Yamato, issued by the draw ability). */
public final class RaidenItems {
	// Same keys, colors and order as the lore moved out of ModItems.
	public static final TransformationItem RAIDEN_SUIT = ModContent.item(
			"raiden_suit",
			new TransformationItem(ModId.of("raiden_shogun"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.LIGHT_PURPLE,
							List.of(new TransformationLore.Line("item.superheroes.raiden_suit.lore.line1", ChatFormatting.LIGHT_PURPLE),
									new TransformationLore.Line("item.superheroes.raiden_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.raiden_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.raiden_suit.lore.untransform", ChatFormatting.RED))))
	);

	// Not in the creative tab: the bound sword is issued by the draw ability, not given freely.
	public static final MusouNoHitotachiItem MUSOU_NO_HITOTACHI = ModContent.item(
			"musou_no_hitotachi",
			new MusouNoHitotachiItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private RaidenItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(RAIDEN_SUIT);
	}
}
