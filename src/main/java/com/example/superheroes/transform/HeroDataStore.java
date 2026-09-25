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
 * coalesced into one packet per player at the end of the server tick.
 */
public final class HeroDataStore {
	/** Ordered after the default phase so a whole tick of resource changes is flushed once. */
	public static final ResourceLocation FLUSH_PHASE = ModId.of("hero_data_flush");

	private HeroDataStore() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, FLUSH_PHASE);
		ServerTickEvents.END_SERVER_TICK.register(FLUSH_PHASE, server -> {
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
