package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.client.player.AbstractClientPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hero-agnostic input seam for {@link FlightPoseMath#directional}: an
 * EMF-owned presentation registers a provider that fills the signed
 * forward/strafe/vertical speeds and the authored-motion boost weight out
 * of its own smoothed render-velocity state (Homelander:
 * {@code HomelanderPoseState}, half-life 3 ticks). The pose tracker
 * consults it only for {@code EmfPresentationOwnership.isOwned} players;
 * a provider that holds no state for the entity returns {@code false} and
 * the caller stays on the legacy {@link FlightPoseMath#target} path, so
 * non-owned entities are untouched.
 */
public final class DirectionalPoseSource {
	private DirectionalPoseSource() {
	}

	/**
	 * Reusable per-entity input holder: the tracker keeps one on its tracked
	 * entry and providers overwrite it each tick — nothing allocates on the
	 * per-tick (let alone per-frame) path.
	 */
	public static final class Input {
		/** Signed speed along the body-yaw forward axis, blocks/tick. */
		public float forward;
		/** Signed speed along the body-yaw right axis, blocks/tick. */
		public float strafe;
		/** Smoothed vertical speed, blocks/tick (+ up). */
		public float vertical;
		/** Authored BOOST blend weight in {@code [0, 1]}. */
		public float boostWeight;
	}

	@FunctionalInterface
	public interface Provider {
		/**
		 * Fill {@code out} with {@code player}'s directional inputs.
		 *
		 * @return {@code true} when {@code out} holds live values;
		 *         {@code false} keeps the caller on the
		 *         {@code FlightPoseMath.target} path.
		 */
		boolean fill(AbstractClientPlayer player, Input out);
	}

	private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

	public static void register(Provider provider) {
		PROVIDERS.add(provider);
	}

	/**
	 * The first provider holding live inputs for {@code player} wins.
	 * {@code false} when nobody fills — a non-owned entity, or a provider
	 * with no state for it yet.
	 */
	public static boolean fill(AbstractClientPlayer player, Input out) {
		for (Provider provider : PROVIDERS) {
			if (provider.fill(player, out)) {
				return true;
			}
		}
		return false;
	}
}
