package com.example.superheroes.effect;

import com.example.superheroes.ability.RaidenSwordDrawAbility;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HeroAttributes;
import net.minecraft.server.level.ServerPlayer;

/**
 * Хелперы по очистке состояния Райден (вызываются из HeroTransformService).
 * RaidenState не persistent и не copyOnDeath — но при untransform надо
 * убрать Yamato из инвентаря и снять burst-модификаторы атрибутов.
 */
public final class RaidenLifecycleController {
	private RaidenLifecycleController() {
	}

	public static void clearOnUntransform(ServerPlayer player) {
		RaidenSwordDrawAbility.removeSword(player);
		HeroAttributes.RAIDEN_BURST.remove(player);
		player.setAttached(ModAttachments.RAIDEN_STATE, RaidenState.EMPTY);
	}
}
