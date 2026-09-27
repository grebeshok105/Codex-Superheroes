package io.github.grebeshok105.codex.hero.raiden.runtime;

import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenSwordDrawAbility;
import net.minecraft.server.level.ServerPlayer;

/**
 * Хелперы по очистке состояния Райден.
 * RaidenState не persistent и не copyOnDeath — но при untransform надо
 * убрать Yamato из инвентаря и снять burst-модификаторы атрибутов.
 */
public final class RaidenLifecycleController {
	private RaidenLifecycleController() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.lifecycle().onHeroClear(RaidenLifecycleController::clearOnUntransform);
	}

	public static void clearOnUntransform(ServerPlayer player) {
		RaidenSwordDrawAbility.removeSword(player);
		RaidenModifiers.RAIDEN_BURST.remove(player);
		player.setAttached(RaidenState.ATTACHMENT, RaidenState.EMPTY);
	}
}
