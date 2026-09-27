package io.github.grebeshok105.codex.client.hero.kratos;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.fx.CustomParticleGate;
import io.github.grebeshok105.codex.client.hero.kratos.hud.KratosRageHud;
import io.github.grebeshok105.codex.client.hero.kratos.state.ClientKratosRageState;
import io.github.grebeshok105.codex.hero.kratos.KratosHero;
import io.github.grebeshok105.codex.hero.kratos.net.KratosRageS2CPayload;
import io.github.grebeshok105.codex.hero.kratos.registry.KratosParticles;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.resources.ResourceLocation;

public record KratosClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return KratosHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(600, ModId.of("spartan_rage"), KratosRageHud::render);
		ctx.particle(KratosParticles.KRATOS_HAND_BURST_1,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ctx.particle(KratosParticles.KRATOS_HAND_BURST_2,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ctx.particle(KratosParticles.KRATOS_HAND_BURST_3,
				sprites -> new CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ctx.receive(KratosRageS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientKratosRageState.update(payload.rage(), payload.active())));
	}
}
