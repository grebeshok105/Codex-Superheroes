package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.core.emf.EmfBridge;
import io.github.grebeshok105.codex.client.core.emf.EmfHeldItemSuppression;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.hero.homelander.HomelanderHero;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

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
		HomelanderEmfVariables.register();
		// EMF must fall back to the vanilla model for everyone we do not own —
		// otherwise every player on the server would render through our jem.
		EmfBridge.registerVanillaModelCondition(uuid -> !EmfPresentationOwnership.isOwned(uuid));
		// The authored bottle replaces the vanilla held item while milk plays.
		EmfHeldItemSuppression.register(HomelanderEmf::hideHeldItemForMilk);
		ClientTickEvents.END_CLIENT_TICK.register(HomelanderPoseState::tick);
	}

	/**
	 * MILK_DRINK on an owned player: arm the authored 6.3 s sequence — the
	 * milk clip clock restarts and the milk weight rises so the jem plays the
	 * bottle, cap and mouth channels.
	 */
	public static void milkStarted(@Nullable Entity entity) {
		if (entity instanceof AbstractClientPlayer player && EmfPresentationOwnership.isOwned(player)) {
			HomelanderPoseState.startMilk(player.getUUID());
		}
	}

	/** MILK_CANCEL (early release): the milk weight returns to 0. */
	public static void milkCancelled(@Nullable Entity entity) {
		if (entity != null) {
			HomelanderPoseState.cancelMilk(entity.getUUID());
		}
	}

	/** The third-person held item hides while the authored bottle is out. */
	private static boolean hideHeldItemForMilk(LivingEntity entity) {
		return entity instanceof AbstractClientPlayer player
				&& EmfPresentationOwnership.isOwned(player)
				&& HomelanderPoseState.milkWeight(player.getUUID()) > 0f;
	}
}
