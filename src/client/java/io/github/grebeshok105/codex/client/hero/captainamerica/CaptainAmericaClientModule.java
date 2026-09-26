package io.github.grebeshok105.codex.client.hero.captainamerica;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.render.ShieldProjectileRenderer;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.hero.CaptainAmericaHero;
import net.minecraft.resources.ResourceLocation;

public record CaptainAmericaClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return CaptainAmericaHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.entityRenderer(ModEntities.SHIELD_PROJECTILE, ShieldProjectileRenderer::new);
	}
}
