package io.github.grebeshok105.codex.hero.regulus.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.regulus.runtime.LionHeartController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Lion's Heart — the void defense toggle. Activation only starts a cast: the authored
 * {@code lion_heart_activation} clip winds up for 14 ticks (trigger frame 0.70s) and
 * the shield — the {@code ALLOW_DAMAGE} void, the projectile freeze, the overheat —
 * exists from the fire tick on, inside {@link LionHeartController}. Toggling marks
 * the ability active immediately; a pre-fire hit cancels the cast for free
 * ({@code damageInterrupts}) and never raises the shield.
 */
public final class LionHeartAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("lion_heart");

	/** Authored trigger: frame 42 @60fps (0.70s). */
	private static final int FIRE_TICK = 14;
	/** Authored clip length 1.60s — the cast session outlives the trigger until then. */
	private static final int CAST_UNTIL_TICK = 32;
	private static final float DRAIN_PER_TICK = 10f;

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		// The contract is parameterless and cannot read the shield state — the 10/t
		// drain runs in onTickActive, charged only once the shield is up.
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		boolean started = RegulusCastState.startCast(player, new RegulusCastState.Spec(
				ID, FIRE_TICK, CAST_UNTIL_TICK, 0f, 0, true,
				() -> LionHeartController.activate(player), () -> {
				}));
		if (!started) {
			return false;
		}
		Vec3 origin = player.position();
		VfxFx.event(player, RegulusVfxIds.ANIM_LION_HEART_ACTIVATION, origin, origin, 1f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		LionHeartController.drainActive(player, DRAIN_PER_TICK);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		// A windup toggle-off aborts this cast for free (the guard keeps another
		// ability's live cast untouched); the shield teardown lives in the controller.
		if (RegulusCastState.isCasting(player, ID)) {
			RegulusCastState.cancel(player);
		}
		LionHeartController.deactivate(player);
	}
}
