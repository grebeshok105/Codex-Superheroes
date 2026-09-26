package io.github.grebeshok105.codex.hero.scorpion;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.scorpion.ability.ScorpionFireTeleportAbility;
import io.github.grebeshok105.codex.hero.scorpion.ability.ScorpionHellBreathAbility;
import io.github.grebeshok105.codex.hero.scorpion.ability.ScorpionHellfireAbility;
import io.github.grebeshok105.codex.hero.scorpion.ability.ScorpionSpearAbility;
import io.github.grebeshok105.codex.hero.scorpion.net.ScorpionFxS2CPayload;
import io.github.grebeshok105.codex.hero.scorpion.runtime.ScorpionController;
import io.github.grebeshok105.codex.hero.scorpion.sound.ScorpionSounds;

public final class ScorpionModule implements HeroModule {
	private final ScorpionHero hero = new ScorpionHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ScorpionSpearAbility());
		ctx.abilities().register(new ScorpionHellfireAbility());
		ctx.abilities().register(new ScorpionFireTeleportAbility());
		ctx.abilities().register(new ScorpionHellBreathAbility());
		ScorpionItems.register(ctx.content());
		ScorpionSounds.register();
		ctx.payloads().s2c(ScorpionFxS2CPayload.TYPE, ScorpionFxS2CPayload.STREAM_CODEC);
		ScorpionController.register(ctx);
	}
}
