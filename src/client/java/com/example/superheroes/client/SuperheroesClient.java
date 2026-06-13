package com.example.superheroes.client;

import com.example.superheroes.client.hud.AbilityBarHud;
import com.example.superheroes.client.hud.HeroInfoPanelHud;
import com.example.superheroes.client.hud.HotbarOverrideHud;
import com.example.superheroes.client.hud.BloodRainHud;
import com.example.superheroes.client.hud.EvangelionZoomHud;
import com.example.superheroes.client.hud.JarvisOverlayHud;
import com.example.superheroes.client.hud.MadnessHudOverlay;
import com.example.superheroes.client.hud.RadialMenuHud;
import com.example.superheroes.client.hud.ReactorOverlayHud;
import com.example.superheroes.client.hud.ScreenFlashHud;
import com.example.superheroes.client.hud.SunWindupHud;
import com.example.superheroes.client.fx.ScreenShakeManager;
import com.example.superheroes.client.fx.WallImpactDebrisManager;
import com.example.superheroes.client.network.ClientNetworking;
import com.example.superheroes.client.render.HomelanderBossRenderer;
import com.example.superheroes.client.render.RemOniHornFeatureRenderer;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.client.render.CosmicBeamRenderer;
import com.example.superheroes.client.render.LaserBeamRenderer;
import com.example.superheroes.client.render.LocalLaserOverlay;
import com.example.superheroes.client.render.RepulsorBeamRenderer;
import com.example.superheroes.client.render.lightning.SuperheroLightningRenderer;
import com.example.superheroes.client.screen.BindingsScreen;
import com.example.superheroes.network.ActivateAbilityC2SPayload;
import com.example.superheroes.network.HeroMeleeChargeC2SPayload;
import com.example.superheroes.network.SuperJumpC2SPayload;
import com.example.superheroes.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class SuperheroesClient implements ClientModInitializer {
	private static boolean meleeChargeSent;
	private static int meleeChargeTicks;

	@Override
	public void onInitializeClient() {
		ModKeys.init();
		ClientNetworking.init();
		com.example.superheroes.client.iris.IrisShaderBridge.restoreAfterCrashIfNeeded();
		ClientTickEvents.END_CLIENT_TICK.register(com.example.superheroes.client.ClientMirrorDimensionState::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> com.example.superheroes.client.ClientPandoraDeathState.tick());
		com.example.superheroes.client.render.WildShaders.register();
		LaserBeamRenderer.register();
		RepulsorBeamRenderer.register();
		com.example.superheroes.client.render.IronManEspRenderer.register();
		CosmicBeamRenderer.register();
		LocalLaserOverlay.register();
		EntityRendererRegistry.register(EntityType.LIGHTNING_BOLT, SuperheroLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOMELANDER_BOSS, HomelanderBossRenderer::new);
		EntityRendererRegistry.register(ModEntities.SHADOW_SOLDIER, com.example.superheroes.client.render.ShadowSoldierRenderer::new);
		EntityRendererRegistry.register(ModEntities.KAGE_BUNSHIN, com.example.superheroes.client.render.KageBunshinRenderer::new);
		EntityRendererRegistry.register(ModEntities.SHIELD_PROJECTILE, com.example.superheroes.client.render.ShieldProjectileRenderer::new);
		EntityRendererRegistry.register(ModEntities.SMART_MISSILE, com.example.superheroes.client.render.SmartMissileRenderer::new);
		EntityRendererRegistry.register(ModEntities.RAM, com.example.superheroes.client.render.RamRenderer::new);
		EntityRendererRegistry.register(ModEntities.IRON_LEGION_DRONE, com.example.superheroes.client.render.IronLegionDroneRenderer::new);
		// Horde entity renderers — vanilla models matched to each mob's texture UV
		// (custom geo/textures are mismatched imports → garbled UVs, deferred to a proper import PR).
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.CRAWLER, com.example.superheroes.client.render.horde.GenericHordeRenderer.spider("crawler", 0.4f, 0.55f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.LURKER, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("lurker", 0.4f, 0.72f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.SPITTER, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("spitter", 0.35f, 0.62f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.SWOOPER, com.example.superheroes.client.render.horde.GenericHordeRenderer.ghast("swooper", 0.4f, 0.8f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.STALKER, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("stalker", 0.4f, 0.82f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTOR, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("infector", 0.3f, 0.51f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.PARASITIC_HOUND, com.example.superheroes.client.render.horde.GenericHordeRenderer.cow("parasitic_hound", 0.35f, 0.5f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTED_ZOMBIE, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("infected_zombie", 0.5f, 1.0f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTED_SKELETON, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("infected_skeleton", 0.5f, 1.0f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTED_SPIDER, com.example.superheroes.client.render.horde.GenericHordeRenderer.spider("infected_spider", 0.7f, 1.0f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTED_CREEPER, com.example.superheroes.client.render.horde.GenericHordeRenderer.creeper("infected_creeper", 0.5f, 1.0f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.VOID_PARASITE, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("void_parasite", 0.35f, 0.67f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.HOLLOW_VILLAGER, com.example.superheroes.client.render.horde.GenericHordeRenderer.villager("hollow_villager", 0.5f, 1.0f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTED_CATTLE, com.example.superheroes.client.render.horde.GenericHordeRenderer.cow("infected_cattle", 0.5f, 1.0f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.BROODMOTHER, com.example.superheroes.client.render.horde.GenericHordeRenderer.spider("broodmother", 0.9f, 1.25f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.CORRUPTED_GOLEM, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("corrupted_golem", 0.8f, 1.38f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.HIVEMIND, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("hivemind", 0.6f, 1.03f));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.LEVIATHAN, com.example.superheroes.client.render.horde.GenericHordeRenderer.humanoid("leviathan", 1.0f, 1.54f));
		com.example.superheroes.horde.entity.HordeEntities hordeRef = null; // static init trigger
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.INFECTED_HOMELANDER, com.example.superheroes.client.render.horde.InfectedHomelanderRenderer::new);
		// Horde bomb projectiles render as thrown items.
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.ACID_BOMB,
				ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0f, false));
		EntityRendererRegistry.register(com.example.superheroes.horde.entity.HordeEntities.FIRE_BOMB,
				ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0f, false));
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerRenderer playerRenderer) {
				registrationHelper.register(new RemOniHornFeatureRenderer(playerRenderer));
				registrationHelper.register(new com.example.superheroes.client.render.ReinhardScabbardLayer(playerRenderer));
				registrationHelper.register(new com.example.superheroes.client.render.IronManNanoFormLayer(playerRenderer));
				registrationHelper.register(new com.example.superheroes.client.render.NanoSuitUpLayer(playerRenderer));
			}
		});
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
			// Spectator mode: hide the entire mod HUD
			net.minecraft.client.Minecraft hudMc = net.minecraft.client.Minecraft.getInstance();
			if (hudMc.player != null && hudMc.player.isSpectator()) {
				return;
			}
			JarvisOverlayHud.render(graphics, tracker);
			com.example.superheroes.client.hud.JarvisDetectionHud.render(graphics, tracker);
			HeroInfoPanelHud.render(graphics, tracker);
			HotbarOverrideHud.render(graphics, tracker);
			AbilityBarHud.render(graphics, tracker);
			com.example.superheroes.client.hud.SpartanRageHud.render(graphics, tracker);
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
			com.example.superheroes.client.hud.AbilitiesTooltipHud.render(graphics, tracker);
			com.example.superheroes.client.hud.PandoraDeathTitleHud.render(graphics, tracker);
			com.example.superheroes.client.hud.HordeDebugOverlay.render(graphics, tracker);
			{
				int[] mcOff = com.example.superheroes.client.hud.HudLayoutManager.offset(
						com.example.superheroes.client.hud.HudLayoutManager.MELEE_CHARGE);
				graphics.pose().pushPose();
				graphics.pose().translate(mcOff[0], mcOff[1], 0);
				com.example.superheroes.client.hud.MeleeChargeHud.render(graphics, tracker);
				graphics.pose().popPose();
			}
			com.example.superheroes.client.hud.ReinhardSwordDeathOverlay.render(graphics, tracker);
			com.example.superheroes.client.hud.ReinhardDarknessOverlay.render(graphics, tracker);
			// Топовый слой: чёрная вспышка Зеркального измерения прячет фриз Iris.reload().
			com.example.superheroes.client.hud.MirrorWarpFlashHud.render(graphics, tracker);
		});

		ClientTickEvents.START_CLIENT_TICK.register(SuperheroesClient::tickHeroMeleeCharge);
		ClientTickEvents.START_CLIENT_TICK.register(SuperheroesClient::tickThinkMarkDash);
		ClientTickEvents.END_CLIENT_TICK.register(com.example.superheroes.client.ClientNanoSuitUpState::clientTick);
		ClientTickEvents.END_CLIENT_TICK.register(com.example.superheroes.client.hud.JarvisDetectionHud::tick);
		ClientTickEvents.END_CLIENT_TICK.register(SuperheroesClient::tickNanoWeaponSelect);
		ClientTickEvents.END_CLIENT_TICK.register(SuperheroesClient::tickEspToggle);
		ClientTickEvents.END_CLIENT_TICK.register(SuperheroesClient::tickRepulsorCharge);

		// "HUD" button in the pause menu -> drag editor for all HUD elements
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
				net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(
						new com.example.superheroes.client.screen.NeonButton(scaledWidth - 92, 8, 84, 20,
								net.minecraft.network.chat.Component.translatable("hud.superheroes.edit.open"),
								b -> client.setScreen(new com.example.superheroes.client.screen.HudEditScreen()),
								0xFF8E7BFF, true));
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ScreenShakeManager.tick();
			WallImpactDebrisManager.tick(client.level);
			com.example.superheroes.client.fx.FlightTrailManager.tick(client);
			HeroInfoPanelHud.tick();
			AbilityBarHud.tick();
			com.example.superheroes.client.hud.AbilitiesTooltipHud.tick();
			RadialMenuHud.animTick();
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
			// Raw GLFW polling: vanilla KeyMapping.MAP allows one mapping per key, so our
			// L / 3 / 4 / 5 binds conflict with vanilla and consumeClick() is unreliable.
			while (ModKeys.TOGGLE_TOOLTIPS.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()) {
					com.example.superheroes.client.hud.AbilitiesTooltipHud.toggleVisible();
				}
			}
			for (int i = 0; i < ModKeys.ABILITY_SLOTS.length; i++) {
				RawKeys.drain(ModKeys.ABILITY_SLOTS[i]);
				// Ability keys always fire; 3/4/5 also switch hotbar slots — intended.
				if (RawKeys.pressed(ModKeys.ABILITY_SLOTS[i])
						&& client.player != null && ClientHeroState.data().hasHero()) {
					List<ResourceLocation> abilities = ClientAbilityFilter.visible();
					if (i < abilities.size()) {
						ClientPlayNetworking.send(new ActivateAbilityC2SPayload(abilities.get(i)));
					}
				}
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			com.example.superheroes.client.ClientMirrorDimensionState.onDisconnect();
			com.example.superheroes.client.ClientPandoraDeathState.onDisconnect();
			com.example.superheroes.client.ClientThanosState.clear();
			com.example.superheroes.client.ClientNanoFormState.clear();
			com.example.superheroes.client.ClientThinkMarkState.clear();
			com.example.superheroes.client.hud.JarvisDetectionHud.clear();
			ClientFlightState.clearAll();
			ClientRemDemonismState.clearAll();
			ClientReinhardDarknessState.clearAll();
			ClientMeleeChargeState.clearAll();
			meleeChargeSent = false;
			meleeChargeTicks = 0;
		});

	}

	private static boolean thinkMarkUseWasDown = false;

	/** While the Omni-Man grab is active, RMB (use) launches the dash/slam. */
	private static void tickNanoWeaponSelect(Minecraft client) {
		if (client.player == null || !ClientHeroState.data().hasHero()
				|| !com.example.superheroes.hero.IronManHero.ID.equals(ClientHeroState.data().heroId())) {
			return;
		}
		while (ModKeys.NANO_WEAPON.consumeClick()) {
			com.example.superheroes.client.ClientNanoWeaponState.cycle(1);
		}
	}

	private static void tickEspToggle(Minecraft client) {
		if (client.player == null || !ClientHeroState.data().hasHero()
				|| !com.example.superheroes.hero.IronManHero.ID.equals(ClientHeroState.data().heroId())) {
			return;
		}
		while (ModKeys.ESP_TOGGLE.consumeClick()) {
			com.example.superheroes.client.render.IronManEspRenderer.cycleMode();
		}
	}

	private static void tickRepulsorCharge(Minecraft client) {
		boolean ironMan = client.player != null && ClientHeroState.data().hasHero()
				&& com.example.superheroes.hero.IronManHero.ID.equals(ClientHeroState.data().heroId());
		boolean sneaking = client.player != null && client.player.isShiftKeyDown();
		com.example.superheroes.client.ClientRepulsorChargeState.clientTick(ironMan, sneaking);
	}

	private static void tickThinkMarkDash(Minecraft client) {
		if (client.player == null || client.level == null) {
			thinkMarkUseWasDown = false;
			return;
		}
		boolean grabbing = com.example.superheroes.client.ClientThinkMarkState.isActive(client.player.getUUID());
		boolean useDown = client.screen == null && client.options.keyUse.isDown();
		if (grabbing && useDown && !thinkMarkUseWasDown) {
			ClientPlayNetworking.send(new com.example.superheroes.network.ThinkMarkDashC2SPayload());
		}
		thinkMarkUseWasDown = useDown;
	}

	private static void tickHeroMeleeCharge(Minecraft client) {
		boolean shouldCharge = client.player != null
				&& client.level != null
				&& client.screen == null
				&& ClientHeroState.data().hasHero()
				&& client.options.keyUse.isDown()
				&& canChargeWithHands(client.player);
		if (shouldCharge && !meleeChargeSent) {
			ClientPlayNetworking.send(new HeroMeleeChargeC2SPayload(HeroMeleeChargeC2SPayload.ACTION_START, 0, -1));
			meleeChargeSent = true;
			meleeChargeTicks = 0;
			ClientMeleeChargeState.update(true, 0);
			return;
		}
		if (shouldCharge) {
			meleeChargeTicks = Math.min(com.example.superheroes.physics.ImpactChargeRules.CAP_TICKS, meleeChargeTicks + 1);
			ClientMeleeChargeState.update(true, meleeChargeTicks);
			return;
		}
		if (meleeChargeSent) {
			boolean release = client.player != null
					&& client.level != null
					&& client.screen == null
					&& ClientHeroState.data().hasHero();
			int action = release ? HeroMeleeChargeC2SPayload.ACTION_RELEASE : HeroMeleeChargeC2SPayload.ACTION_CANCEL;
			ClientPlayNetworking.send(new HeroMeleeChargeC2SPayload(action, meleeChargeTicks, hoveredEntityId(client)));
			meleeChargeSent = false;
			meleeChargeTicks = 0;
		}
		ClientMeleeChargeState.clearAll();
	}

	private static boolean canChargeWithHands(net.minecraft.world.entity.player.Player player) {
		if (player.isUsingItem()) {
			return false;
		}
		// Заряженные удары только на пустых кулаках: предмет в основной руке полностью отключает тиры.
		return player.getMainHandItem().isEmpty() && chargeFriendly(player.getOffhandItem());
	}

	private static boolean chargeFriendly(net.minecraft.world.item.ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}
		if (stack.getItem() instanceof net.minecraft.world.item.BlockItem
				|| stack.getItem() instanceof com.example.superheroes.item.RoyalIcicleItem) {
			return false;
		}
		return stack.getUseAnimation() == net.minecraft.world.item.UseAnim.NONE;
	}

	private static int hoveredEntityId(Minecraft client) {
		if (client.hitResult != null && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY
				&& client.hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
			return entityHit.getEntity().getId();
		}
		return -1;
	}
}
