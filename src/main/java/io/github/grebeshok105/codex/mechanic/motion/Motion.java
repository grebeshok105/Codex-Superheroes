package io.github.grebeshok105.codex.mechanic.motion;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Server-side velocity changes that clients must see this tick. */
public final class Motion {
	public enum Sync {
		/** Mark only: vanilla's entity tracker sends the velocity on its next update. */
		MARK,
		/** Mark and push a motion packet to the entity itself right now if it is a player (client-authoritative movement). */
		MARK_AND_SEND_TO_PLAYER
	}

	private Motion() {
	}

	public static void set(Entity entity, Vec3 velocity, Sync sync) {
		entity.setDeltaMovement(velocity);
		entity.hurtMarked = true;
		if (sync == Sync.MARK_AND_SEND_TO_PLAYER && entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	public static void add(Entity entity, Vec3 delta, Sync sync) {
		set(entity, entity.getDeltaMovement().add(delta), sync);
	}
}
