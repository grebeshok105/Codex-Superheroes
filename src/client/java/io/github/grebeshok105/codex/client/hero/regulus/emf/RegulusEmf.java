package io.github.grebeshok105.codex.client.hero.regulus.emf;

import io.github.grebeshok105.codex.client.core.emf.EmfBridge;
import io.github.grebeshok105.codex.client.core.emf.EmfHeldItemSuppression;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.UUID;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Regulus's EMF entry point: claims every rendered Regulus player as
 * EMF-owned (the merged {@code player.jem}/{@code player_slim.jem} rg_* rig
 * carries the authored clips), registers the clip-time/weight variables,
 * and starts the per-player {@link RegulusPoseState} clocks from the
 * {@code regulus/anim/*} one-shot events routed here by
 * {@code RegulusFx}. Clip priority is cast clips > evangelium idle >
 * combat idle > vanilla rest, per Amendment A(b).
 *
 * <p>Everything is gated on {@link EmfBridge#isAvailable()} — no EMF, no
 * variables, no ownership claims, no clocks.
 */
public final class RegulusEmf {
	private RegulusEmf() {
	}

	public static void register(HeroClientContext ctx) {
		if (!EmfBridge.isAvailable()) {
			return;
		}
		EmfPresentationOwnership.register(RegulusEmf::isRegulusPlayer);
		RegulusEmfVariables.register();
		EmfBridge.registerVanillaModelCondition(
				uuid -> !EmfPresentationOwnership.isOwned(uuid));
		EmfHeldItemSuppression.register(entity ->
				entity instanceof AbstractClientPlayer player
						&& EmfPresentationOwnership.isOwned(player));
		ClientTickEvents.END_CLIENT_TICK.register(RegulusPoseState::tick);
	}

	private static boolean isRegulusPlayer(AbstractClientPlayer player) {
		return RegulusHero.ID.equals(SkinResolver.heroIdFor(player));
	}

	// -- anim event entry points (called by RegulusFx one-shots) ---------------

	public static void embraceCastStarted(@Nullable Entity source) {
		start(source, ClipStart.EMBRACE);
	}

	public static void lionHeartActivationStarted(@Nullable Entity source) {
		start(source, ClipStart.LION_HEART);
	}

	public static void lionRoarStarted(@Nullable Entity source) {
		start(source, ClipStart.LION_ROAR);
	}

	public static void debrisKickStarted(@Nullable Entity source) {
		start(source, ClipStart.DEBRIS_KICK);
	}

	public static void maniaCastStarted(@Nullable Entity source) {
		start(source, ClipStart.MANIA);
	}

	public static void counterAttackStarted(@Nullable Entity source) {
		start(source, ClipStart.COUNTER);
	}

	public static void evangeliumActivationStarted(@Nullable Entity source) {
		start(source, ClipStart.EVANGELIUM_ON);
	}

	public static void evangeliumDeactivationStarted(@Nullable Entity source) {
		start(source, ClipStart.EVANGELIUM_OFF);
	}

	private enum ClipStart {
		EMBRACE, LION_HEART, LION_ROAR, DEBRIS_KICK, MANIA, COUNTER, EVANGELIUM_ON, EVANGELIUM_OFF
	}

	private static void start(@Nullable Entity source, ClipStart clip) {
		if (!(source instanceof AbstractClientPlayer player)) {
			return;
		}
		UUID uuid = player.getUUID();
		switch (clip) {
			case EMBRACE -> RegulusPoseState.startEmbraceCast(uuid);
			case LION_HEART -> RegulusPoseState.startLionHeartActivation(uuid);
			case LION_ROAR -> RegulusPoseState.startLionRoar(uuid);
			case DEBRIS_KICK -> RegulusPoseState.startDebrisKick(uuid);
			case MANIA -> RegulusPoseState.startManiaCast(uuid);
			case COUNTER -> RegulusPoseState.startCounterAttack(uuid);
			case EVANGELIUM_ON -> RegulusPoseState.startEvangeliumActivation(uuid);
			case EVANGELIUM_OFF -> RegulusPoseState.startEvangeliumDeactivation(uuid);
		}
	}
}
