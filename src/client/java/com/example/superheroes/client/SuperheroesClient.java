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
import com.example.superheroes.client.render.IronManEspRenderer;
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
		EntityRendererRegistry.register(ModEntities.RAM, com.example.superheroes.client.render.RamRenderer::new);
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerRenderer playerRenderer) {
				registrationHelper.register(new RemOniHornFeatureRenderer(playerRenderer));
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
			JarvisOverlayHud.render(graphics, tracker);
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
			com.example.superheroes.client.hud.MeleeChargeHud.render(graphics, tracker);
			com.example.superheroes.client.hud.ReinhardSwordDeathOverlay.render(graphics, tracker);
			com.example.superheroes.client.hud.ReinhardDarknessOverlay.render(graphics, tracker);
		});

		ClientTickEvents.START_CLIENT_TICK.register(SuperheroesClient::tickHeroMeleeCharge);
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			// Hotbar lock: swallow 1-9 slot keys so vanilla can't switch slots (scroll wheel still works)
			if (HotbarLockState.isLocked() && client.player != null && ClientHeroState.data().hasHero()) {
				for (net.minecraft.client.KeyMapping key : client.options.keyHotbarSlots) {
					while (key.consumeClick()) {
						// consumed — hotbar is locked
					}
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ScreenShakeManager.tick();
			WallImpactDebrisManager.tick(client.level);
			com.example.superheroes.client.fx.FlightTrailManager.tick(client);
			HeroInfoPanelHud.tick();
			AbilityBarHud.tick();
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
			while (ModKeys.HOTBAR_LOCK.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()) {
					HotbarLockState.toggle();
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

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientFlightState.clearAll();
			ClientRemDemonismState.clearAll();
			ClientReinhardDarknessState.clearAll();
			ClientMeleeChargeState.clearAll();
			meleeChargeSent = false;
			meleeChargeTicks = 0;
		});

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
