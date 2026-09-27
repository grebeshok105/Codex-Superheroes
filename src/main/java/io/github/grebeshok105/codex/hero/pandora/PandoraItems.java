package io.github.grebeshok105.codex.hero.pandora;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class PandoraItems {
	// persisted id kept from the Doctor Strange era — do not rename the string.
	public static final TransformationItem PANDORA_SUIT = ModContent.item(
			"doctor_strange_suit",
			new TransformationItem(ModId.of("pandora"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					new TransformationLore(ChatFormatting.DARK_RED,
							List.of(new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.line1", ChatFormatting.RED),
									new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.line2", ChatFormatting.DARK_GRAY)),
							List.of(new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.usage", ChatFormatting.YELLOW),
									new TransformationLore.Line("item.superheroes.doctor_strange_suit.lore.untransform", ChatFormatting.GOLD))))
	);

	private PandoraItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(PANDORA_SUIT);
	}
}
