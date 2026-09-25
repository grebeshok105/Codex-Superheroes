package com.example.superheroes.transform;

import com.example.superheroes.ModId;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.ModNetworking;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.UnaryOperator;

/**
 * The only writer of {@link ModAttachments#HERO_DATA}.
 *
 * <p>Every change is a read-modify-write of the current value, so no caller can resurrect state by
 * writing back a copy it read earlier in the tick (audit B2). Changes to the hero, bindings or active
 * set are synced immediately to keep ordering with other payloads; energy/mana-only changes are
 * coalesced into one packet per player every {@value #RESOURCE_FLUSH_INTERVAL} ticks.
 */
public final class HeroDataStore {
	/** Ordered after the default phase so a whole tick of resource changes is flushed once. */
	public static final ResourceLocation FLUSH_PHASE = ModId.of("hero_data_flush");
	/** Dirty resource state is flushed at most once per this many ticks. */
	private static final int RESOURCE_FLUSH_INTERVAL = 10;

	private HeroDataStore() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, FLUSH_PHASE);
		ServerTickEvents.END_SERVER_TICK.register(FLUSH_PHASE, server -> {
			// Energy regen dirties the attachment every tick; flushing the dirty
			// flag every RESOURCE_FLUSH_INTERVAL ticks keeps HUD updates smooth
			// enough (0.5s) without a packet per tick per player (audit §3).
			if (server.getTickCount() % RESOURCE_FLUSH_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				flushResources(player);
			}
		});
	}

	public static HeroData get(Player player) {
		return player.getAttachedOrCreate(ModAttachments.HERO_DATA);
	}

	/**
	 * Applies {@code change} to the current value and schedules the matching sync.
	 * {@code change} must be a pure function of its argument.
	 */
	public static HeroData update(ServerPlayer player, UnaryOperator<HeroData> change) {
		HeroData before = get(player);
		HeroData after = change.apply(before);
		if (after.equals(before)) {
			return before;
		}
		player.setAttached(ModAttachments.HERO_DATA, after);
		syncPublicHero(player, after.heroId());
		if (sameShape(before, after)) {
			player.setAttached(ModAttachments.HERO_DATA_RESOURCES_DIRTY, Boolean.TRUE);
		} else {
			sendFull(player, after);
		}
		return after;
	}

	/** Sends the full state now even if nothing changed, e.g. on join or respawn. */
	public static void syncFull(ServerPlayer player) {
		sendFull(player, get(player));
	}

	/**
	 * Back-fills {@link ModAttachments#PUBLIC_HERO} from {@link ModAttachments#HERO_DATA} —
	 * covers heroes transformed before the synced attachment existed (audit B14).
	 */
	public static void syncPublicHero(ServerPlayer player) {
		syncPublicHero(player, get(player).heroId());
	}

	private static void syncPublicHero(ServerPlayer player, ResourceLocation heroId) {
		if (heroId == null) {
			player.removeAttached(ModAttachments.PUBLIC_HERO);
		} else {
			player.setAttached(ModAttachments.PUBLIC_HERO, heroId);
		}
	}

	private static void sendFull(ServerPlayer player, HeroData data) {
		player.removeAttached(ModAttachments.HERO_DATA_RESOURCES_DIRTY);
		ModNetworking.syncHeroData(player, data);
	}

	private static void flushResources(ServerPlayer player) {
		if (player.removeAttached(ModAttachments.HERO_DATA_RESOURCES_DIRTY) != null) {
			ModNetworking.syncResources(player, get(player));
		}
	}

	private static boolean sameShape(HeroData a, HeroData b) {
		return a.currentHero().equals(b.currentHero())
				&& a.abilityBindings().equals(b.abilityBindings())
				&& a.activeAbilities().equals(b.activeAbilities());
	}
}
