package io.github.grebeshok105.codex.hero.doomsday;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.core.transform.TransformationLore;
import io.github.grebeshok105.codex.hero.doomsday.item.KryptoniteShardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.List;

public final class DoomsdayItems {
	// Same keys, colors and order as the lore rows that lived in ModItems.
	private static final TransformationLore GENOME_LORE = new TransformationLore(ChatFormatting.DARK_PURPLE,
			List.of(new TransformationLore.Line("item.superheroes.doomsday_genome.lore.line1", ChatFormatting.LIGHT_PURPLE),
					new TransformationLore.Line("item.superheroes.doomsday_genome.lore.line2", ChatFormatting.DARK_GRAY)),
			List.of(new TransformationLore.Line("item.superheroes.doomsday_genome.lore.usage", ChatFormatting.RED),
					new TransformationLore.Line("item.superheroes.doomsday_genome.lore.untransform", ChatFormatting.GOLD)));

	public static final TransformationItem DOOMSDAY_GENOME = ModContent.item("doomsday_genome",
			new TransformationItem(ModId.of("doomsday"), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC),
					GENOME_LORE));

	public static final KryptoniteShardItem KRYPTONITE_SHARD = ModContent.item("kryptonite_shard",
			new KryptoniteShardItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

	private DoomsdayItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(DOOMSDAY_GENOME);
		content.creativeTab(KRYPTONITE_SHARD);
	}
}
