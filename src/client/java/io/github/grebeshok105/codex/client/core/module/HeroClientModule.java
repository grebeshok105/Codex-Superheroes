package io.github.grebeshok105.codex.client.core.module;

import net.minecraft.resources.ResourceLocation;

public interface HeroClientModule {
	ResourceLocation heroId();

	void register(HeroClientContext ctx);
}
