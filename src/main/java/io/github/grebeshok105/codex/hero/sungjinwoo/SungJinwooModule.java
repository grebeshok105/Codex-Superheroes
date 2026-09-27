package io.github.grebeshok105.codex.hero.sungjinwoo;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.effect.ThanosStoneRewardController;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.AriseAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.MonarchsDomainAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.RulersAuthorityAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.SacrificeAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.ShadowExchangeAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.ShadowExtractionAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.entity.ShadowSoldierEntity;
import io.github.grebeshok105.codex.hero.sungjinwoo.entity.SungJinwooEntities;
import io.github.grebeshok105.codex.hero.sungjinwoo.net.SungShadowArmyS2CPayload;
import io.github.grebeshok105.codex.hero.sungjinwoo.registry.SungJinwooDamageTypes;
import io.github.grebeshok105.codex.hero.sungjinwoo.runtime.MonarchsDomainController;
import io.github.grebeshok105.codex.hero.sungjinwoo.runtime.SungJinwooController;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;

import java.util.List;

public final class SungJinwooModule implements HeroModule {
	private final SungJinwooHero hero = new SungJinwooHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return SungJinwooDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		// Attachment + entity register eagerly inside module bootstrap, matching the timing
		// they had under ModAttachments.init()/ModEntities.init().
		SungJinwooAttachments.init();
		SungJinwooEntities.register();
		SungJinwooItems.register(ctx.content());

		ctx.abilities().register(new AriseAbility());
		ctx.abilities().register(new ShadowExchangeAbility());
		ctx.abilities().register(new SacrificeAbility());
		ctx.abilities().register(new RulersAuthorityAbility());
		ctx.abilities().register(new ShadowExtractionAbility());
		ctx.abilities().register(new MonarchsDomainAbility());

		ctx.payloads().s2c(SungShadowArmyS2CPayload.TYPE, SungShadowArmyS2CPayload.STREAM_CODEC);

		// Sung Jin-Woo→REALITY stone reward, registered by the hero that owns the drop row.
		ThanosStoneRewardController.registerHeroStone(SungJinwooHero.ID, InfinityStoneType.REALITY);

		SungJinwooController.register(ctx);
		// Entity leaf asks the runtime for legitimacy through a wired contract (no entity→runtime edge).
		ShadowSoldierEntity.setArmyMembership(SungJinwooController::isArmyMember);
		ctx.ticks().player(SungJinwooController::tickPlayer);
		ctx.ticks().player(MonarchsDomainController::tickPlayer);
	}
}
