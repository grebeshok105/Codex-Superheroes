package io.github.grebeshok105.codex.client.core.emf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Hero-agnostic registry answering "does EMF own this player's model?".
 * A hero module registers a predicate (e.g. Homelander via
 * {@code SkinResolver.heroIdFor}); owned players bypass the legacy
 * {@code heroPose} pipeline and let the EMF jem drive the model instead.
 * Ownership is always false when EMF is absent.
 */
public final class EmfPresentationOwnership {

	private static final List<Predicate<AbstractClientPlayer>> PREDICATES = new CopyOnWriteArrayList<>();

	private EmfPresentationOwnership() {
	}

	public static void register(Predicate<AbstractClientPlayer> predicate) {
		PREDICATES.add(predicate);
	}

	public static boolean isOwned(AbstractClientPlayer player) {
		if (!EmfBridge.isAvailable()) {
			return false;
		}
		for (Predicate<AbstractClientPlayer> predicate : PREDICATES) {
			if (predicate.test(player)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOwned(UUID uuid) {
		if (!EmfBridge.isAvailable()) {
			return false;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return false;
		}
		List<AbstractClientPlayer> players = level.players();
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).getUUID().equals(uuid)) {
				return isOwned(players.get(i));
			}
		}
		return false;
	}
}
