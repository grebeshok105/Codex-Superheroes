package com.example.superheroes;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.command.SuperheroesCommands;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.item.ModItemGroups;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.lifecycle.EntityControlLock;
import com.example.superheroes.lifecycle.PlayerLifecycle;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroDataStore;
import com.example.superheroes.transform.HeroTransformService;
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
		com.example.superheroes.lifecycle.PassiveReconciler.init();
		ModEffects.init();
		com.example.superheroes.bootstrap.HeroModules.bootstrap(com.example.superheroes.core.module.CoreModuleContext.INSTANCE);
		com.example.superheroes.entity.ModEntities.init();
		com.example.superheroes.horde.entity.HordeEntities.init();
		com.example.superheroes.item.ModDataComponents.init();
		ModItems.init();
		ModItemGroups.init();
		ModParticles.init();
		ModSounds.init();
		ModNetworking.init();
		HeroDataStore.init();
		SuperheroesCommands.init();

		com.example.superheroes.lifecycle.HeroTickDispatcher.init();

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
