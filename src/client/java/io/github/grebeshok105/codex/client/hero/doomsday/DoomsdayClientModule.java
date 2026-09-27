package io.github.grebeshok105.codex.client.hero.doomsday;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.doomsday.hud.DoomsdayGlitchHud;
import io.github.grebeshok105.codex.client.hero.doomsday.state.ClientDoomsdayState;
import io.github.grebeshok105.codex.hero.doomsday.DoomsdayHero;
import io.github.grebeshok105.codex.hero.doomsday.net.DoomsdayProgressS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record DoomsdayClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return DoomsdayHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(1600, ModId.of("doomsday_glitch"), DoomsdayGlitchHud::render);
		ctx.receive(DoomsdayProgressS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientDoomsdayState.update(payload.tier(), payload.adaptations())));
	}
}
