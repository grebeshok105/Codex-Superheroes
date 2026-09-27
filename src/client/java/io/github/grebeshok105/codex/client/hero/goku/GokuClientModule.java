package io.github.grebeshok105.codex.client.hero.goku;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.goku.GokuHero;
import io.github.grebeshok105.codex.hero.goku.registry.GokuParticles;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.resources.ResourceLocation;

public record GokuClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return GokuHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.particle(GokuParticles.GOKU_KI_AURA, EndRodParticle.Provider::new);
		ctx.particle(GokuParticles.GOKU_KAMEHAMEHA_CORE, EndRodParticle.Provider::new);
		ctx.particle(GokuParticles.GOKU_KAMEHAMEHA_TRAIL, EndRodParticle.Provider::new);
	}
}
