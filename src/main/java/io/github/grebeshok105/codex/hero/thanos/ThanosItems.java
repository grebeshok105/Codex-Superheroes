package io.github.grebeshok105.codex.hero.thanos;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityGauntletItem;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStoneItem;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStoneType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ThanosItems {
	public static final InfinityGauntletItem INFINITY_GAUNTLET = ModContent.item(
			"infinity_gauntlet",
			new InfinityGauntletItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem POWER_STONE = ModContent.item(
			InfinityStoneType.POWER.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.POWER, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem SPACE_STONE = ModContent.item(
			InfinityStoneType.SPACE.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.SPACE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem REALITY_STONE = ModContent.item(
			InfinityStoneType.REALITY.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.REALITY, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem SOUL_STONE = ModContent.item(
			InfinityStoneType.SOUL.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.SOUL, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem TIME_STONE = ModContent.item(
			InfinityStoneType.TIME.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.TIME, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	public static final InfinityStoneItem MIND_STONE = ModContent.item(
			InfinityStoneType.MIND.getItemRegistryName(),
			new InfinityStoneItem(InfinityStoneType.MIND, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	private ThanosItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(INFINITY_GAUNTLET);
		content.creativeTab(POWER_STONE);
		content.creativeTab(SPACE_STONE);
		content.creativeTab(REALITY_STONE);
		content.creativeTab(SOUL_STONE);
		content.creativeTab(TIME_STONE);
		content.creativeTab(MIND_STONE);
	}
}
