package com.example.superheroes.client.hero.reinhard;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.network.ReinhardCeremonyS2CPayload;
import com.example.superheroes.network.ReinhardDarknessS2CPayload;
import com.example.superheroes.network.ReinhardSwordGateS2CPayload;
import com.example.superheroes.network.ReinhardSwordKillS2CPayload;
import com.example.superheroes.network.ReinhardTimeSlowS2CPayload;
import com.example.superheroes.network.ReinhardWishOptionsS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public record ReinhardClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ReinhardHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(ReinhardWishOptionsS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					Minecraft mc = Minecraft.getInstance();
					mc.setScreen(com.example.superheroes.client.screen.ReinhardWishScreen.of(
							payload.damageTypeIds(), payload.adaptedDamageTypeIds(),
							payload.wishesUsed(), payload.wishesMax()));
				}));

		ctx.receive(ReinhardCeremonyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardCeremonyState.update(
						payload.active(), payload.progress())));

		ctx.receive(ReinhardSwordGateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardSwordGateState.update(
						payload.ready(), payload.progress())));

		ctx.receive(ReinhardSwordKillS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardSwordKillState.update(payload.active())));

		ctx.receive(ReinhardDarknessS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardDarknessState.activate(payload.durationTicks())));

		ctx.receive(ReinhardTimeSlowS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientReinhardTimeSlowState.update(payload.active())));
	}
}
