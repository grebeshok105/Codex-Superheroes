package io.github.grebeshok105.codex.client.hero.invincible;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.InvincibleHero;
import net.minecraft.resources.ResourceLocation;

public record InvincibleClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return InvincibleHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
