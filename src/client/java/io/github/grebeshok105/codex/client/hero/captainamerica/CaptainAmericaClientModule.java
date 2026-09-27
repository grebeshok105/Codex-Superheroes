package io.github.grebeshok105.codex.client.hero.captainamerica;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.captainamerica.render.ShieldProjectileRenderer;
import io.github.grebeshok105.codex.hero.captainamerica.CaptainAmericaHero;
import io.github.grebeshok105.codex.hero.captainamerica.entity.CaptainAmericaEntities;
import io.github.grebeshok105.codex.hero.captainamerica.registry.CaptainAmericaParticles;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.resources.ResourceLocation;

public record CaptainAmericaClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return CaptainAmericaHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.entityRenderer(CaptainAmericaEntities.SHIELD_PROJECTILE, ShieldProjectileRenderer::new);
		ctx.particle(CaptainAmericaParticles.CAP_SHIELD_TRAIL, EndRodParticle.Provider::new);
		ctx.particle(CaptainAmericaParticles.CAP_SHIELD_SLAM_BURST, EndRodParticle.Provider::new);
	}
}
