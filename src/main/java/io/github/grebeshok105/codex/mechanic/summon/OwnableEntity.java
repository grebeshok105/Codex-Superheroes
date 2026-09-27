package io.github.grebeshok105.codex.mechanic.summon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A summoned entity owned by a player (Sung's shadow soldiers; later Rem's Ram,
 * Iron Man's legion drones, clones). The binding is by UUID so it survives chunk
 * unload and server restarts; {@link #getOwner()} additionally gates on the owner
 * being a live player.
 */
public interface OwnableEntity {
	@Nullable
	UUID getOwnerId();

	void setOwnerId(@Nullable UUID ownerId);

	/** The owner player, or null when the owner is absent, offline, or dead. */
	@Nullable
	Player getOwner();

	/** True when {@code entity} is this summonable's owner. */
	default boolean isOwner(Entity entity) {
		UUID id = getOwnerId();
		return id != null && id.equals(entity.getUUID());
	}
}
