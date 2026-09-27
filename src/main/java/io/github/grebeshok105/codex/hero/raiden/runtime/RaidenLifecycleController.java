package io.github.grebeshok105.codex.hero.raiden.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponItem;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeapons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Хелперы по очистке состояния Райден.
 * RaidenState не persistent и не copyOnDeath — но при untransform надо
 * убрать Yamato из инвентаря и снять burst-модификаторы атрибутов.
 */
public final class RaidenLifecycleController {
	private static final ResourceLocation YAMATO_ID = ModId.of("musou_no_hitotachi");

	private RaidenLifecycleController() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.lifecycle().onHeroClear(RaidenLifecycleController::clearOnUntransform);
	}

	public static void clearOnUntransform(ServerPlayer player) {
		// Same call the ability's removeSword did — leaf lookup by literal id keeps
		// runtime off the ability/ leaf (module DAG: leafs reference runtime, never back).
		BoundWeapons.revoke(player, yamato());
		RaidenModifiers.RAIDEN_BURST.remove(player);
		player.setAttached(RaidenState.ATTACHMENT, RaidenState.EMPTY);
	}

	private static BoundWeaponItem yamato() {
		return (BoundWeaponItem) BuiltInRegistries.ITEM.get(YAMATO_ID);
	}
}
