package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.ability.RaidenSwordDrawAbility;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.AbilityScopedModifiers;
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
		AbilityScopedModifiers.RAIDEN_BURST.remove(player);
		player.setAttached(ModAttachments.RAIDEN_STATE, RaidenState.EMPTY);
	}
}
