package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.core.camera.ThirdPersonFraming;
import io.github.grebeshok105.codex.client.core.emf.EmfBridge;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.client.core.flight.DirectionalPoseSource;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.hero.homelander.HomelanderHero;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Homelander EMF integration: owns the player model (and only Homelander's)
 * for EMF rendering — the generated jem under {@code assets/minecraft/emf/cem/}
 * drives the model while the legacy {@code heroPose} pipeline and legacy
 * clip plays are bypassed for owned players. Covers remote players too:
 * ownership resolves through {@link SkinResolver}, which reads the
 * {@code PUBLIC_HERO} attachment for non-local players.
 */
public final class HomelanderEmf {

	private HomelanderEmf() {
	}

	public static void register(HeroClientContext ctx) {
		if (!EmfBridge.isAvailable()) {
			return;
		}
		EmfPresentationOwnership.register(
				player -> HomelanderHero.ID.equals(SkinResolver.heroIdFor(player)));
		// Directional flight inputs for the pose tracker (§7 stage 5):
		// the same smoothed render velocity the boost latch consumes.
		DirectionalPoseSource.register((player, out) ->
				HomelanderPoseState.fillDirectional(player.getUUID(), out));
		// Third-person framing (§7 stage 6): centre the detached camera on
		// the body while the EMF presentation is engaged.
		ThirdPersonFraming.register(HomelanderPoseState::cameraOffset);
		HomelanderEmfVariables.register();
		// EMF must fall back to the vanilla model for everyone we do not own —
		// otherwise every player on the server would render through our jem.
		EmfBridge.registerVanillaModelCondition(uuid -> !EmfPresentationOwnership.isOwned(uuid));
		ClientTickEvents.END_CLIENT_TICK.register(HomelanderPoseState::tick);
	}

	/** {@code CLAP} event: start the authored clip for the source player. */
	public static void clapStarted(Entity entity) {
		if (EmfBridge.isAvailable() && entity instanceof AbstractClientPlayer player) {
			HomelanderPoseState.startClap(player.getUUID());
		}
	}

	/** {@code CLAP_CANCEL} event: the hit was dropped — release the clip early. */
	public static void clapCancelled(Entity entity) {
		if (EmfBridge.isAvailable() && entity instanceof AbstractClientPlayer player) {
			HomelanderPoseState.cancelClap(player.getUUID());
		}
	}
}
