package io.github.grebeshok105.codex.combat;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Shared predicate for hostile-target selection in ability AoE scans.
 *
 * <p>Player-vs-player targets honor vanilla rules through {@link ServerPlayer#canHarmPlayer}:
 * {@code pvp=false} and same-team players without friendly fire are skipped, matching
 * what {@link ServerPlayer#hurt} would reject anyway.
 */
public final class TargetFilters {
	private TargetFilters() {
	}

	/**
	 * Whether {@code target} is a living, non-spectator entity other than
	 * {@code attacker} that {@code attacker} may harm under PvP/team rules.
	 */
	public static boolean harmableBy(LivingEntity target, Entity attacker) {
		if (target == attacker || !target.isAlive() || target.isSpectator()) {
			return false;
		}
		return !(target instanceof ServerPlayer victim && attacker instanceof Player player)
				|| victim.canHarmPlayer(player);
	}

	/**
	 * {@link #harmableBy} as a scan predicate, additionally skipping creative players
	 * (the most common condition shared by the per-ability copies).
	 */
	public static Predicate<LivingEntity> hostileTo(Entity attacker) {
		return target -> harmableBy(target, attacker) && notCreative(target);
	}

	public static boolean notCreative(LivingEntity target) {
		return !(target instanceof Player player && player.isCreative());
	}
}
