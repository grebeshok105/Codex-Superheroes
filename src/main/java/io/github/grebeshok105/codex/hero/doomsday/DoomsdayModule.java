package io.github.grebeshok105.codex.hero.doomsday;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.lifecycle.MobEffectGates;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.doomsday.ability.ChargeTackleAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomGripAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdayBerserkAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdayBoneSpikeAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdayRoarAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdaySmashAbility;
import io.github.grebeshok105.codex.hero.doomsday.net.DoomsdayProgressS2CPayload;
import io.github.grebeshok105.codex.hero.doomsday.registry.DoomsdayDamageTypes;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomGripController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayAdaptationController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayEffectAdaptationController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayFootstepsController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayKryptoniteController;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayTierController;
import io.github.grebeshok105.codex.hero.doomsday.sound.DoomsdaySounds;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.List;

public final class DoomsdayModule implements HeroModule {
	private final DoomsdayHero hero = new DoomsdayHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return DoomsdayDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new DoomsdaySmashAbility());
		ctx.abilities().register(new DoomsdayRoarAbility());
		ctx.abilities().register(new DoomsdayBoneSpikeAbility());
		ctx.abilities().register(new ChargeTackleAbility());
		ctx.abilities().register(new DoomsdayBerserkAbility());
		ctx.abilities().register(new DoomGripAbility());
		DoomsdayItems.register(ctx.content());
		DoomsdayAttachments.init();
		DoomsdaySounds.init();
		ctx.payloads().s2c(DoomsdayProgressS2CPayload.TYPE, DoomsdayProgressS2CPayload.STREAM_CODEC);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
				DoomsdayCommands.register(dispatcher));
		MobEffectGates.register(DoomsdayEffectAdaptationController::allow);
		DoomsdayAdaptationController.register(ctx);
		DoomsdayTierController.register(ctx);
		DoomsdayKryptoniteController.register(ctx);
		ctx.lifecycle().onLeave(DoomGripController::clear);
		ctx.lifecycle().onDeath(DoomGripController::clear);
		ctx.lifecycle().onHeroClear(DoomGripController::clear);
		ctx.lifecycle().onServerStopped(server -> DoomGripController.resetAll());
		ctx.ticks().global(server -> DoomGripController.serverTick());
		ctx.ticks().global(DoomsdayKryptoniteController::serverTick);
		ctx.ticks().player((server, p, data) -> ChargeTackleAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> DoomsdayFootstepsController.tickPlayer(p));
	}
}
