package io.github.grebeshok105.codex.core.content;

import net.minecraft.world.level.ItemLike;

/** Content hooks a hero module may register through its {@code HeroModuleContext}. */
public interface ContentRegistrar {
	/** Adds the item to the mod's creative tab; it lands after the legacy items. */
	void creativeTab(ItemLike item);
}
