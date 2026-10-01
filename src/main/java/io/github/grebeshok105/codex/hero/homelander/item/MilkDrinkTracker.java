package io.github.grebeshok105.codex.hero.homelander.item;

import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Falling-edge detector for the milk drink. Vanilla ends a use through several
 * paths: the release-use packet runs {@code LivingEntity.releaseUsingItem}, but
 * a hotbar slot swap or an offhand swap only run {@code stopUsingItem}, which
 * never calls {@code Item.releaseUsing}. Watching the use flag each server tick
 * covers every stop shape with exactly one MILK_CANCEL and no per-path plumbing:
 * {@code MilkBottleItem.use} arms the mark, {@code finishUsingItem} disarms it,
 * and any armed use that ends unfinished fires the cancel for tracking + self.
 * Death, disconnect and hero-clear drop the mark silently — the entity is gone,
 * so there is nothing left to cancel on clients.
 */
public final class MilkDrinkTracker {

	// Boolean payload only to satisfy OwnedSessionMap's non-null value.
	private static final OwnedSessionMap<UUID, Boolean> DRINKING =
			OwnedSessionMap.create(LifecycleRegistrar.global(),
					EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));

	private MilkDrinkTracker() {
	}

	/** Arms the detector; call when the bottle's server-side use begins. */
	public static void started(ServerPlayer player) {
		DRINKING.put(player.getUUID(), player.getUUID(), Boolean.TRUE);
	}

	/** A completed use never cancels: disarm before finishUsingItem lands its effect. */
	public static void finished(ServerPlayer player) {
		DRINKING.remove(player.getUUID());
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		UUID id = player.getUUID();
		if (!DRINKING.containsKey(id)) {
			return;
		}
		if (player.isUsingItem() && player.getUseItem().getItem() instanceof MilkBottleItem) {
			return;
		}
		DRINKING.remove(id);
		VfxFx.event(player, HomelanderVfxIds.MILK_CANCEL,
				player.position(), player.position(), 1f);
	}
}
