package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.bootstrap.HeroClientModules;
import io.github.grebeshok105.codex.client.core.hud.HudLayers;
import io.github.grebeshok105.codex.client.core.render.PlayerLayers;
import io.github.grebeshok105.codex.client.hud.AbilitiesTooltipHud;
import io.github.grebeshok105.codex.client.hud.AbilityBarHud;
import io.github.grebeshok105.codex.client.hud.ChatHudMovable;
import io.github.grebeshok105.codex.client.hud.EffectsHudMovable;
import io.github.grebeshok105.codex.client.hud.HeroInfoPanelHud;
import io.github.grebeshok105.codex.client.hud.HordeDebugOverlay;
import io.github.grebeshok105.codex.client.hud.HotbarOverrideHud;
import io.github.grebeshok105.codex.client.hud.MeleeChargeHud;
import io.github.grebeshok105.codex.client.hud.RadialMenuHud;
import io.github.grebeshok105.codex.client.hud.ScreenFlashHud;
import io.github.grebeshok105.codex.client.fx.ScreenShakeManager;
import io.github.grebeshok105.codex.client.fx.WallImpactDebrisManager;
import io.github.grebeshok105.codex.client.network.ClientNetworking;
import io.github.grebeshok105.codex.client.render.HomelanderBossRenderer;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.client.render.CosmicBeamRenderer;
import io.github.grebeshok105.codex.client.render.LaserBeamRenderer;
import io.github.grebeshok105.codex.client.render.LocalLaserOverlay;
import io.github.grebeshok105.codex.client.render.lightning.SuperheroLightningRenderer;
import io.github.grebeshok105.codex.client.screen.BindingsScreen;
import io.github.grebeshok105.codex.network.ActivateAbilityC2SPayload;
import io.github.grebeshok105.codex.network.HeroMeleeChargeC2SPayload;
import io.github.grebeshok105.codex.network.SuperJumpC2SPayload;
import io.github.grebeshok105.codex.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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
		HeroClientModules.bootstrap();
		io.github.grebeshok105.codex.client.iris.IrisShaderBridge.restoreAfterCrashIfNeeded();
		ClientTickEvents.END_CLIENT_TICK.register(client -> io.github.grebeshok105.codex.client.iris.IrisShaderBridge.tickCrashRestore());
		ClientTickEvents.END_CLIENT_TICK.register(io.github.grebeshok105.codex.client.ClientMirrorDimensionState::tick);
		ClientHeroDimsWatcher.init();
		ClientTickEvents.END_CLIENT_TICK.register(client -> io.github.grebeshok105.codex.client.ClientPandoraDeathState.tick());
		io.github.grebeshok105.codex.client.render.WildShaders.register();
		LaserBeamRenderer.register();
		CosmicBeamRenderer.register();
		LocalLaserOverlay.register();
		EntityRendererRegistry.register(EntityType.LIGHTNING_BOLT, SuperheroLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOMELANDER_BOSS, HomelanderBossRenderer::new);
		// Horde entity renderers — vanilla models matched to each mob's texture UV
		// (custom geo/textures are mismatched imports → garbled UVs, deferred to a proper import PR).
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.CRAWLER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.spider("crawler", 0.4f, 0.55f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.LURKER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("lurker", 0.4f, 0.72f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.SPITTER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("spitter", 0.35f, 0.62f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.SWOOPER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.ghast("swooper", 0.4f, 0.8f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.STALKER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("stalker", 0.4f, 0.82f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTOR, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("infector", 0.3f, 0.51f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.PARASITIC_HOUND, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.cow("parasitic_hound", 0.35f, 0.5f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTED_ZOMBIE, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("infected_zombie", 0.5f, 1.0f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTED_SKELETON, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("infected_skeleton", 0.5f, 1.0f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTED_SPIDER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.spider("infected_spider", 0.7f, 1.0f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTED_CREEPER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.creeper("infected_creeper", 0.5f, 1.0f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.VOID_PARASITE, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("void_parasite", 0.35f, 0.67f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.HOLLOW_VILLAGER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.villager("hollow_villager", 0.5f, 1.0f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTED_CATTLE, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.cow("infected_cattle", 0.5f, 1.0f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.BROODMOTHER, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.spider("broodmother", 0.9f, 1.25f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.CORRUPTED_GOLEM, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("corrupted_golem", 0.8f, 1.38f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.HIVEMIND, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("hivemind", 0.6f, 1.03f));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.LEVIATHAN, io.github.grebeshok105.codex.client.render.horde.GenericHordeRenderer.humanoid("leviathan", 1.0f, 1.54f));
		io.github.grebeshok105.codex.horde.entity.HordeEntities hordeRef = null; // static init trigger
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.INFECTED_HOMELANDER, io.github.grebeshok105.codex.client.render.horde.InfectedHomelanderRenderer::new);
		// Horde bomb projectiles render as thrown items.
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.ACID_BOMB,
				ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0f, false));
		EntityRendererRegistry.register(io.github.grebeshok105.codex.horde.entity.HordeEntities.FIRE_BOMB,
				ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0f, false));
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerRenderer playerRenderer) {
				PlayerLayers.registerAll(playerRenderer, registrationHelper);
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
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SWORD_EXPLOSION,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SPARKS,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.DARK_STAR,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.PURPLE_FLAME,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLACK_FLAME,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.DAZZLING,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SUN_PARTICLE,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SOUL_SPARK,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.NIGHTFALL,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.CHAOS_ORB,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.KRATOS_HAND_BURST_1,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.KRATOS_HAND_BURST_2,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.KRATOS_HAND_BURST_3,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.ANOMALY_SLICE,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.JIWALD_EFFECT,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.FULA_PARTICLE,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.SHAMAK,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLUE_FLAME,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		ParticleFactoryRegistry.getInstance().register(ModParticles.MOONVEIL,
				sprites -> new io.github.grebeshok105.codex.client.fx.CustomParticleGate(sprites, EndRodParticle.Provider::new));
		io.github.grebeshok105.codex.client.config.SuperheroesClientConfig.load();

		HudLayers.init();

		HudLayers.registerMovable(300, ModId.of("hero_panel"), HeroInfoPanelHud::render, HeroInfoPanelHud.INSTANCE);
		HudLayers.registerMovable(400, ModId.of("hotbar"), HotbarOverrideHud::render, HotbarOverrideHud.INSTANCE);
		HudLayers.registerMovable(500, ModId.of("ability_bar"), AbilityBarHud::render, AbilityBarHud.INSTANCE);
		// Vanilla chat and effect icons are shifted by mixins, not drawn by a mod layer:
		// movable-only entries so the HUD editor keeps all 7 cards.
		HudLayers.registerMovable(550, ModId.of("chat"), (graphics, delta) -> {
		}, ChatHudMovable.INSTANCE);
		HudLayers.registerMovable(560, ModId.of("effects"), (graphics, delta) -> {
		}, EffectsHudMovable.INSTANCE);
		HudLayers.register(700, ModId.of("radial_menu"), RadialMenuHud::render);
		HudLayers.register(800, ModId.of("screen_flash"), ScreenFlashHud::render);
		HudLayers.registerMovable(1800, ModId.of("tooltips"), AbilitiesTooltipHud::render, AbilitiesTooltipHud.INSTANCE);
		HudLayers.register(2000, ModId.of("horde_debug"), HordeDebugOverlay::render);
		HudLayers.registerMovable(2100, ModId.of("melee_charge"), MeleeChargeHud::render, MeleeChargeHud.INSTANCE);

		ClientTickEvents.START_CLIENT_TICK.register(SuperheroesClient::tickHeroMeleeCharge);
		ClientTickEvents.START_CLIENT_TICK.register(SuperheroesClient::tickThinkMarkDash);

		// "HUD" button in the pause menu -> drag editor for all HUD elements
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
				net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(
						new io.github.grebeshok105.codex.client.screen.NeonButton(scaledWidth - 92, 8, 84, 20,
								net.minecraft.network.chat.Component.translatable("hud.superheroes.edit.open"),
								b -> client.setScreen(new io.github.grebeshok105.codex.client.screen.HudEditScreen()),
								0xFF8E7BFF, true));
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ScreenShakeManager.tick();
			WallImpactDebrisManager.tick(client.level);
			io.github.grebeshok105.codex.client.fx.FlightTrailManager.tick(client);
			HeroInfoPanelHud.tick();
			AbilityBarHud.tick();
			io.github.grebeshok105.codex.client.hud.AbilitiesTooltipHud.tick();
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
					client.setScreen(new io.github.grebeshok105.codex.client.screen.VfxSettingsScreen());
				}
			}

			while (ModKeys.SUPER_JUMP.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(SuperJumpC2SPayload.INSTANCE);
				}
			}
			// Raw GLFW polling: vanilla KeyMapping.MAP allows one mapping per key, so our
			// L / 3 / 4 / 5 binds conflict with vanilla and consumeClick() is unreliable.
			while (ModKeys.TOGGLE_TOOLTIPS.consumeClick()) {
				if (client.player != null && ClientHeroState.data().hasHero()) {
					io.github.grebeshok105.codex.client.hud.AbilitiesTooltipHud.toggleVisible();
				}
			}
			for (int i = 0; i < ModKeys.ABILITY_SLOTS.length; i++) {
				RawKeys.drain(ModKeys.ABILITY_SLOTS[i]);
				// Ability keys always fire; 3/4/5 also switch hotbar slots — intended.
				if (RawKeys.pressed(ModKeys.ABILITY_SLOTS[i])
						&& client.player != null && ClientHeroState.data().hasHero()) {
					List<ResourceLocation> abilities = ClientAbilityVisibility.visible();
					if (i < abilities.size()) {
						ClientPlayNetworking.send(new ActivateAbilityC2SPayload(abilities.get(i)));
					}
				}
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			// Every Client*State holder registers here via its static block —
			// no per-class list to go stale (audit B15).
			ClientSessionState.resetAll();
			meleeChargeSent = false;
			meleeChargeTicks = 0;
			thinkMarkUseWasDown = false;
		});

	}

	private static boolean thinkMarkUseWasDown = false;

	/** While the Omni-Man grab is active, RMB (use) launches the dash/slam. */
	private static void tickThinkMarkDash(Minecraft client) {
		if (client.player == null || client.level == null) {
			thinkMarkUseWasDown = false;
			return;
		}
		boolean grabbing = io.github.grebeshok105.codex.client.ClientThinkMarkState.isActive(client.player.getUUID());
		boolean useDown = client.screen == null && client.options.keyUse.isDown();
		if (grabbing && useDown && !thinkMarkUseWasDown) {
			ClientPlayNetworking.send(new io.github.grebeshok105.codex.network.ThinkMarkDashC2SPayload());
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
			meleeChargeTicks = Math.min(io.github.grebeshok105.codex.physics.ImpactChargeRules.CAP_TICKS, meleeChargeTicks + 1);
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
				|| stack.getItem() instanceof io.github.grebeshok105.codex.item.RoyalIcicleItem) {
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
