package io.github.grebeshok105.codex.content.boss.homelander;

import io.github.grebeshok105.codex.content.boss.homelander.entity.HomelanderBossEntities;
import io.github.grebeshok105.codex.content.boss.homelander.registry.HomelanderBossDamageTypes;
import io.github.grebeshok105.codex.core.module.ContentModule;
import io.github.grebeshok105.codex.core.module.ContentModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;

import java.util.List;

/**
 * The Homelander boss fight: the entity and its AI goals, the vought_signal summon item,
 * the admin-only spawn egg, and the eight homelander_* damage types.
 */
public final class HomelanderBossModule implements ContentModule {
	@Override
	public void register(ContentModuleContext ctx) {
		HomelanderBossEntities.init();
		HomelanderBossItems.register(ctx.content());
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return HomelanderBossDamageTypes.SPECS;
	}
}
