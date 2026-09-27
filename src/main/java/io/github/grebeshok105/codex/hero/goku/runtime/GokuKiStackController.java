package io.github.grebeshok105.codex.hero.goku.runtime;

import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;

public final class GokuKiStackController {
	public static final int MAX_STACKS = 3;

	// No lifecycle cleanup ever existed for stacks — damage resets, consume and the
	// (currently unwired) clear() stay the only drop points.
	private static final OwnedSessionMap<UUID, Integer> STACKS =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private GokuKiStackController() {
	}

	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player && damageTaken > 0.5f) {
				STACKS.remove(player.getUUID());
			}
		});
	}

	public static int getStacks(ServerPlayer player) {
		Integer s = STACKS.get(player.getUUID());
		return s == null ? 0 : s;
	}

	public static int addStack(ServerPlayer player) {
		int next = Math.min(MAX_STACKS, getStacks(player) + 1);
		STACKS.put(player.getUUID(), player.getUUID(), next);
		return next;
	}

	public static void clear(ServerPlayer player) {
		STACKS.remove(player.getUUID());
	}

	public static int consume(ServerPlayer player) {
		Integer s = STACKS.remove(player.getUUID());
		return s == null ? 0 : s;
	}
}
