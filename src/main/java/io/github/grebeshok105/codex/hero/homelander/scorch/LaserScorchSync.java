package io.github.grebeshok105.codex.hero.homelander.scorch;

import io.github.grebeshok105.codex.core.net.FxBroadcast;
import io.github.grebeshok105.codex.core.net.ScorchMark;
import io.github.grebeshok105.codex.core.net.ScorchMarksS2CPayload;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Scorch-mark send paths. A new mark goes only to players around it
 * ({@value #BROADCAST_RADIUS} blocks); players entering the dimension — join,
 * respawn or world change — get the whole state re-streamed in
 * {@link ScorchMarksS2CPayload#SYNC_CHUNK}-sized chunks, the first one flagged
 * {@code reset} so the client drops the previous dimension's marks.
 */
public final class LaserScorchSync {
	private static final double BROADCAST_RADIUS = 128.0;

	private LaserScorchSync() {
	}

	public static void register(HeroModuleContext ctx) {
		ctx.lifecycle().onJoin(LaserScorchSync::sendAll);
		ctx.lifecycle().onRespawn(LaserScorchSync::sendAll);
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
				(player, origin, destination) -> sendAll(player));
	}

	/** Broadcasts a freshly created mark to every player within {@value #BROADCAST_RADIUS} blocks. */
	public static void broadcastNew(ServerLevel level, ScorchMark mark) {
		FxBroadcast.around(level, Vec3.atCenterOf(mark.pos()), BROADCAST_RADIUS,
				new ScorchMarksS2CPayload(List.of(mark), false));
	}

	/** Sends the full mark state of the player's current dimension in chunks of {@link ScorchMarksS2CPayload#SYNC_CHUNK}. */
	public static void sendAll(ServerPlayer player) {
		LaserScorchData data = LaserScorchData.get(player.serverLevel());
		List<ScorchMark> chunk = new ArrayList<>(ScorchMarksS2CPayload.SYNC_CHUNK);
		boolean[] first = {true};
		data.forEachChronological(mark -> {
			chunk.add(mark);
			if (chunk.size() == ScorchMarksS2CPayload.SYNC_CHUNK) {
				send(player, chunk, first[0]);
				first[0] = false;
				chunk.clear();
			}
		});
		// Always send at least one packet: a client switching dimensions must
		// still get its reset even when the new dimension has no marks yet.
		if (first[0] || !chunk.isEmpty()) {
			send(player, chunk, first[0]);
		}
	}

	private static void send(ServerPlayer player, List<ScorchMark> chunk, boolean reset) {
		ServerPlayNetworking.send(player, new ScorchMarksS2CPayload(chunk, reset));
	}
}
