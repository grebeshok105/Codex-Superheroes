package io.github.grebeshok105.codex.client.hero.battlebeast;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.BattleBeastHero;
import net.minecraft.resources.ResourceLocation;

public record BattleBeastClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return BattleBeastHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
