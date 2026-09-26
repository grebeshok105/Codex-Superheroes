package io.github.grebeshok105.codex;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.command.SuperheroesCommands;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.Heroes;
import io.github.grebeshok105.codex.item.ModItemGroups;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.lifecycle.EntityControlLock;
import io.github.grebeshok105.codex.lifecycle.PlayerLifecycle;
import io.github.grebeshok105.codex.network.ModNetworking;
import io.github.grebeshok105.codex.particle.ModParticles;
import io.github.grebeshok105.codex.sound.ModSounds;
import io.github.grebeshok105.codex.transform.HeroDataStore;
import io.github.grebeshok105.codex.transform.HeroTransformService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperheroesMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(ModId.MOD_ID);

	@Override
	public void onInitialize() {
		ModAttachments.init();
		EntityControlLock.init();
		PlayerLifecycle.init();
		io.github.grebeshok105.codex.lifecycle.PassiveReconciler.init();
		ModEffects.init();
		io.github.grebeshok105.codex.bootstrap.HeroModules.bootstrap(io.github.grebeshok105.codex.core.module.CoreModuleContext.INSTANCE);
		io.github.grebeshok105.codex.entity.ModEntities.init();
		io.github.grebeshok105.codex.horde.entity.HordeEntities.init();
		io.github.grebeshok105.codex.item.ModDataComponents.init();
		ModItems.init();
		ModItemGroups.init();
		ModParticles.init();
		ModSounds.init();
		ModNetworking.init();
		HeroDataStore.init();
		SuperheroesCommands.init();

		io.github.grebeshok105.codex.lifecycle.HeroTickDispatcher.init();

		// Global (not hero-owned) death handling: a dead hero untransforms — a hero that
		// adapts through death keeps the transformation via Hero.keepsHeroOnDeath().
		// Stays the last registered AFTER_DEATH listener: PlayerLifecycle's DEATH list
		// (PlayerLifecycle.init above) and every module's own AFTER_DEATH hooks
		// (HeroModules.bootstrap) all run before forceUntransform.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer serverPlayer) {
				Hero hero = Heroes.get(serverPlayer
						.getAttachedOrCreate(ModAttachments.HERO_DATA).heroId());
				if (hero != null && hero.keepsHeroOnDeath()) {
					return;
				}
				HeroTransformService.forceUntransform(serverPlayer);
			}
		});

		LOGGER.info("Superheroes mod initialized");
	}
}
