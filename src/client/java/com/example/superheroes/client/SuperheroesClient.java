package com.example.superheroes.client;

import com.example.superheroes.client.hud.AbilitiesTooltipHud;
import com.example.superheroes.client.hud.BloodRainHud;
import com.example.superheroes.client.hud.EvangelionZoomHud;
import com.example.superheroes.client.hud.JarvisOverlayHud;
import com.example.superheroes.client.hud.MadnessHudOverlay;
import com.example.superheroes.client.hud.RadialMenuHud;
import com.example.superheroes.client.hud.ReactorOverlayHud;
import com.example.superheroes.client.hud.ResourceBarHud;
import com.example.superheroes.client.hud.ScreenFlashHud;
import com.example.superheroes.client.hud.SunWindupHud;
import com.example.superheroes.client.fx.ScreenShakeManager;
import com.example.superheroes.client.network.ClientNetworking;
import com.example.superheroes.client.render.HomelanderBossRenderer;
import com.example.superheroes.client.render.IronManEspRenderer;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.client.render.CosmicBeamRenderer;
import com.example.superheroes.client.render.LaserBeamRenderer;
import com.example.superheroes.client.render.LocalLaserOverlay;
import com.example.superheroes.client.render.RepulsorBeamRenderer;
import com.example.superheroes.client.render.lightning.SuperheroLightningRenderer;
import com.example.superheroes.client.screen.BindingsScreen;
import com.example.superheroes.network.ActivateAbilityC2SPayload;
import com.example.superheroes.network.SuperJumpC2SPayload;
import com.example.superheroes.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class SuperheroesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModKeys.init();
		ClientNetworking.init();
		LaserBeamRenderer.register();
		RepulsorBeamRenderer.register();
		CosmicBeamRenderer.register();
		LocalLaserOverlay.register();
		IronManEspRenderer.register();
		EntityRendererRegistry.register(EntityType.LIGHTNING_BOLT, SuperheroLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOMELANDER_BOSS, HomelanderBossRenderer::new);
		EntityRendererRegistry.register(ModEntities.SHADOW_SOLDIER, com.example.superheroes.client.render.ShadowSoldierRenderer::new);
		EntityRendererRegistry.register(ModEntities.KAGE_BUNSHIN, com.example.superheroes.client.render.KageBunshinRenderer::new);
		EntityRendererRegistry.register(ModEntities.SHIELD_PROJECTILE, com.example.superheroes.client.render.ShieldProjectileRenderer::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.TRANSFORM_SPARK, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.LASER_SPARK, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.REPULSOR_SPARK, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.UNIBEAM_SPARK, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.GOKU_KI_AURA, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.GOKU_KAMEHAMEHA_CORE, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.GOKU_KAMEHAMEHA_TRAIL, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.NARUTO_RASENGAN_SWIRL, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.NARUTO_CLONE_POOF, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.NARUTO_KAWARIMI_SMOKE, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.CAP_SHIELD_TRAIL, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.CAP_SHIELD_SLAM_BURST, EndRodParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.WHITE_BOOM,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SWORD_EXPLOSION,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SPARKS,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.DARK_STAR,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.PURPLE_FLAME,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLACK_FLAME,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.DAZZLING,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SUN_PARTICLE,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SOUL_SPARK,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.NIGHTFALL,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.CHAOS_ORB,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.KRATOS_HAND_BURST_1,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.KRATOS_HAND_BURST_2,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.KRATOS_HAND_BURST_3,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.ANOMALY_SLICE,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.JIWALD_EFFECT,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.FULA_PARTICLE,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SHAMAK,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLUE_FLAME,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.MOONVEIL,
				sprites -> new com.example.superheroes.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		com.example.superheroes.client.config.SuperheroesClientConfig.load();

		HudRenderCallback.EVENT.register((graphics, tracker) -> {
			JarvisOverlayHud.render(graphics, tracker);
			ResourceBarHud.render(graphics, tracker);
			com.example.superheroes.client.hud.SpartanRageHud.render(graphics, tracker);
			AbilitiesTooltipHud.render(graphics, tracker);
			RadialMenuHud.render(graphics, tracker);
			ScreenFlashHud.render(graphics, tracker);
			SunWindupHud.render(graphics, tracker);
			ReactorOverlayHud.render(graphics, tracker);
			MadnessHudOverlay.render(graphics, tracker);
			BloodRainHud.render(graphics, tracker);
			EvangelionZoomHud.render(graphics, tracker);
			com.example.superheroes.client.hud.UraniumThreatHud.render(graphics, tracker);
			com.example.superheroes.client.hud.CracksOverlayHud.render(graphics, tracker);
			com.example.superheroes.client.hud.DoomsdayGlitchHud.render(graphics, tracker);
			com.example.superheroes.client.hud.ReinhardCeremonyOverlay.render(graphics, tracker);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ScreenShakeManager.tick();
			AbilitiesTooltipHud.tick();
			RadialMenuHud.clientTick(client);
			if (ClientMadnessState.isReading() && client.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
				client.setScreen(null);
			}
			while (ModKeys.BINDINGS.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()) {
					client.setScreen(new BindingsScreen());
				}
			}
			while (ModKeys.VFX_SETTINGS.consumeClick()) {
				if (client.player != null) {
					client.setScreen(new com.example.superheroes.client.screen.VfxSettingsScreen());
				}
			}
			while (ModKeys.TOGGLE_TOOLTIPS.consumeClick()) {
				AbilitiesTooltipHud.toggleVisible();
			}
			while (ModKeys.SUPER_JUMP.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(SuperJumpC2SPayload.INSTANCE);
				}
			}
			while (ModKeys.RAIDEN_SWORD_DRAW.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()
						&& com.example.superheroes.hero.RaidenHero.ID.equals(ClientHeroState.heroId())) {
					ClientPlayNetworking.send(new ActivateAbilityC2SPayload(
							com.example.superheroes.ability.AbilityIds.RAIDEN_SWORD_DRAW));
				}
			}
			for (int i = 0; i < ModKeys.ABILITY_SLOTS.length; i++) {
				while (ModKeys.ABILITY_SLOTS[i].consumeClick()) {
					if (client.player == null || !ClientHeroState.data().hasHero()) {
						continue;
					}
					List<ResourceLocation> abilities = ClientAbilityFilter.visible();
					if (i < abilities.size()) {
						ClientPlayNetworking.send(new ActivateAbilityC2SPayload(abilities.get(i)));
					}
				}
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientFlightState.clearAll());

	}
}
