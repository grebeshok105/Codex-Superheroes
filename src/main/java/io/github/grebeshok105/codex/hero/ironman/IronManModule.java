package io.github.grebeshok105.codex.hero.ironman;

import io.github.grebeshok105.codex.hero.ironman.ability.IronManFlightAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.RepulsorAbility;
import io.github.grebeshok105.codex.hero.ironman.runtime.RepulsorChargeController;
import io.github.grebeshok105.codex.hero.ironman.ability.SmartMissileAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.SupersonicAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.UnibeamAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.IronManLegionAbility;
import io.github.grebeshok105.codex.hero.ironman.ability.IronManNanoFormAbility;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManNanoFormController;
import io.github.grebeshok105.codex.hero.ironman.ability.IronManSuitSwitchAbility;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManSuitSyncController;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManAutoEjectController;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManJarvisController;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManReactorTracker;
import io.github.grebeshok105.codex.hero.ironman.runtime.UnibeamController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.ironman.entity.IronManEntities;
import io.github.grebeshok105.codex.hero.ironman.net.JarvisDetectionS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.NanoFormS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.ReactorStateS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.SuitVariantS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.registry.IronManDamageTypes;
import io.github.grebeshok105.codex.hero.ironman.registry.IronManParticles;
import io.github.grebeshok105.codex.hero.ironman.sound.IronManSounds;

import java.util.List;

public final class IronManModule implements HeroModule {
	private final IronManHero hero = new IronManHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return IronManDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		// Module-owned registry content replaces the deleted ModAttachments/ModSounds/ModParticles
		// rows; init() forces the statics now, matching the old eager init order.
		IronManAttachments.init();
		IronManParticles.init();
		IronManSounds.init();
		IronManItems.register(ctx.content());
		IronManEntities.register();
		ctx.payloads().s2c(ReactorStateS2CPayload.TYPE, ReactorStateS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(JarvisDetectionS2CPayload.TYPE, JarvisDetectionS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(SuitVariantS2CPayload.TYPE, SuitVariantS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(NanoFormS2CPayload.TYPE, NanoFormS2CPayload.STREAM_CODEC);

		ctx.abilities().register(new IronManFlightAbility());
		ctx.abilities().register(new SupersonicAbility());
		ctx.abilities().register(new RepulsorAbility());
		ctx.abilities().register(new UnibeamAbility());
		ctx.abilities().register(new SmartMissileAbility());
		ctx.abilities().register(new IronManNanoFormAbility());
		ctx.abilities().register(new IronManSuitSwitchAbility());
		ctx.abilities().register(new IronManLegionAbility());
		IronManJarvisController.register(ctx);
		IronManNanoFormController.register(ctx);
		IronManSuitSyncController.register(ctx);
		ctx.lifecycle().onLeave(p -> UnibeamController.clearState(p.getUUID()));
		ctx.ticks().global(UnibeamController::pruneGonePlayers);
		ctx.ticks().global(IronManJarvisController::serverTick);
		ctx.ticks().player((server, p, data) -> IronManNanoFormController.serverTick(p));
		ctx.ticks().player((server, p, data) -> RepulsorChargeController.serverTick(p));
		ctx.ticks().player(UnibeamController::tickPlayer);
		ctx.ticks().player((server, p, data) -> IronManReactorTracker.tick(p));
		ctx.ticks().player((server, p, data) -> IronManAutoEjectController.tick(p, server.getTickCount()));
	}
}
