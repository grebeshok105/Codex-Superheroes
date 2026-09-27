package io.github.grebeshok105.codex.client.hero.omniman;

import io.github.grebeshok105.codex.client.ClientThinkMarkState;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.omniman.OmnimanHero;
import io.github.grebeshok105.codex.hero.omniman.net.ThinkMarkDashC2SPayload;
import io.github.grebeshok105.codex.hero.omniman.net.ThinkMarkS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public record OmnimanClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return OmnimanHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(ThinkMarkS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientThinkMarkState.update(payload.playerId(), payload.active())));
		ClientTickEvents.START_CLIENT_TICK.register(OmnimanClientModule::tickThinkMarkDash);
	}

	private static boolean thinkMarkUseWasDown = false;

	/** While the Omni-Man grab is active, RMB (use) launches the dash/slam. */
	private static void tickThinkMarkDash(Minecraft client) {
		if (client.player == null || client.level == null) {
			thinkMarkUseWasDown = false;
			return;
		}
		boolean grabbing = ClientThinkMarkState.isActive(client.player.getUUID());
		boolean useDown = client.screen == null && client.options.keyUse.isDown();
		if (grabbing && useDown && !thinkMarkUseWasDown) {
			ClientPlayNetworking.send(new ThinkMarkDashC2SPayload());
		}
		thinkMarkUseWasDown = useDown;
	}
}
