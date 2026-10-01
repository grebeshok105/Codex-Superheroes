package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.resource.ResourceController;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Authored-event cast machine (Regulus rework): one cast session per player, keyed on the
 * owner's UUID in an {@link OwnedSessionMap} so leave, death and hero-clear drop it — with
 * no side effects, because the effect does not exist before the authored fire tick.
 *
 * <p>{@link #startCast} opens a session and is refused while one runs. The PLAYERS-phase
 * tick fires the effect at {@code startTick + fireTick}: it charges {@code activateCost}
 * first (a failed charge cancels the cast for free — interrupt hook, no cooldown), runs
 * {@code onFire}, then arms the cooldown. The record is removed at
 * {@code startTick + castUntilTick}, when the clip has played out. Incoming damage on a
 * cast marked {@code damageInterrupts} cancels it the same free way — the damage itself
 * still lands (interrupt, not block).
 */
public final class RegulusCastState {
	/**
	 * One authored cast: the effect fires at {@code fireTick} ticks after the start, the
	 * session ends at {@code castUntilTick}. {@code onFire} runs on the fire tick after a
	 * successful charge; {@code onInterrupt} runs when the cast ends before firing.
	 */
	public record Spec(ResourceLocation abilityId, int fireTick, int castUntilTick,
					   float activateCost, int cooldownTicks, boolean damageInterrupts,
					   Runnable onFire, Runnable onInterrupt) {
	}

	private static final class Cast {
		private final Spec spec;
		private final long startTick;
		private boolean fired;

		private Cast(Spec spec, long startTick) {
			this.spec = spec;
			this.startTick = startTick;
		}
	}

	private static final OwnedSessionMap<UUID, Cast> CASTS = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));

	private RegulusCastState() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.ticks().player(RegulusCastState::tickPlayer);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || amount <= 0f) {
				return true;
			}
			Cast cast = CASTS.get(player.getUUID());
			if (cast == null || !cast.spec.damageInterrupts()) {
				return true;
			}
			cancel(player);
			return true;
		});
	}

	/** Opens a cast session; {@code false} while any session already runs on this player. */
	public static boolean startCast(ServerPlayer player, Spec spec) {
		if (CASTS.containsKey(player.getUUID())) {
			return false;
		}
		CASTS.put(player.getUUID(), player.getUUID(), new Cast(spec, player.level().getGameTime()));
		return true;
	}

	/** A session for {@code abilityId} is running — windup or post-fire recovery alike. */
	public static boolean isCasting(ServerPlayer player, ResourceLocation abilityId) {
		Cast cast = CASTS.get(player.getUUID());
		return cast != null && cast.spec.abilityId().equals(abilityId);
	}

	/** The {@code abilityId} session already passed its authored fire tick. */
	public static boolean hasFired(ServerPlayer player, ResourceLocation abilityId) {
		Cast cast = CASTS.get(player.getUUID());
		return cast != null && cast.fired && cast.spec.abilityId().equals(abilityId);
	}

	/** Ticks left until the {@code abilityId} session reaches its castUntil deadline; 0 when absent. */
	public static int castTicksLeft(ServerPlayer player, ResourceLocation abilityId) {
		Cast cast = CASTS.get(player.getUUID());
		if (cast == null || !cast.spec.abilityId().equals(abilityId)) {
			return 0;
		}
		long left = cast.startTick + cast.spec.castUntilTick() - player.level().getGameTime();
		return left > 0 ? (int) left : 0;
	}

	/**
	 * Ends the session early — explicit abort, despawn, damage interrupt. A cast that never
	 * fired runs {@code onInterrupt} (the free cancel); a fired one is just dropped — its
	 * effect already happened, so there is nothing to interrupt.
	 */
	public static void cancel(ServerPlayer player) {
		Cast cast = CASTS.remove(player.getUUID());
		if (cast != null && !cast.fired) {
			cast.spec.onInterrupt().run();
		}
	}

	/** PLAYERS-phase tick; the dispatcher already skips dead players. */
	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		Cast cast = CASTS.get(player.getUUID());
		if (cast == null) {
			return;
		}
		long now = player.level().getGameTime();
		if (!cast.fired && now >= cast.startTick + cast.spec.fireTick()) {
			if (ResourceController.charge(player, cast.spec.abilityId(), cast.spec.activateCost()) == null) {
				// The authored event could not be paid for — free cancel, no cooldown.
				cancel(player);
				return;
			}
			cast.fired = true;
			cast.spec.onFire().run();
			AbilityCooldowns.setCooldownTicks(player, cast.spec.abilityId(), cast.spec.cooldownTicks());
		}
		if (now >= cast.startTick + cast.spec.castUntilTick()) {
			CASTS.remove(player.getUUID());
		}
	}
}
