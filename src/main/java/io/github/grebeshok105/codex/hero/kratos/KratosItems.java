package io.github.grebeshok105.codex.hero.kratos;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.ModContent;
import io.github.grebeshok105.codex.hero.kratos.item.BladeOfChaosItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class KratosItems {
	public static final BladeOfChaosItem BLADE_OF_CHAOS = ModContent.item(
			"blade_of_chaos",
			new BladeOfChaosItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
	);

	private KratosItems() {
	}

	static void register(ContentRegistrar content) {
		content.creativeTab(BLADE_OF_CHAOS);
	}
}
