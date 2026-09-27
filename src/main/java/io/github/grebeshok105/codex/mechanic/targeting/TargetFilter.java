package io.github.grebeshok105.codex.mechanic.targeting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Explicit target rules. Every flag defaults to "off" so migrating a call site keeps its old predicate exactly;
 * turning on {@link #respectPvp}/{@link #excludeAllies} is a gameplay change (audit B19) and ships separately.
 */
public record TargetFilter(@Nullable Entity owner, boolean excludeOwner, boolean aliveOnly, boolean excludeSpectators,
		boolean respectPvp, boolean excludeAllies, @Nullable Predicate<LivingEntity> extra) {
	public static TargetFilter of(@Nullable Entity owner) {
		return new TargetFilter(owner, false, false, false, false, false, null);
	}

	public TargetFilter withoutOwner() { return new TargetFilter(owner, true, aliveOnly, excludeSpectators, respectPvp, excludeAllies, extra); }
	public TargetFilter alive() { return new TargetFilter(owner, excludeOwner, true, excludeSpectators, respectPvp, excludeAllies, extra); }
	public TargetFilter withoutSpectators() { return new TargetFilter(owner, excludeOwner, aliveOnly, true, respectPvp, excludeAllies, extra); }
	public TargetFilter and(Predicate<LivingEntity> more) { return new TargetFilter(owner, excludeOwner, aliveOnly, excludeSpectators, respectPvp, excludeAllies, extra == null ? more : extra.and(more)); }

	public boolean test(LivingEntity candidate) {
		if (excludeOwner && candidate == owner) return false;
		if (aliveOnly && !candidate.isAlive()) return false;
		if (excludeSpectators && candidate.isSpectator()) return false;
		if (respectPvp && owner instanceof net.minecraft.server.level.ServerPlayer attacker
				&& candidate instanceof net.minecraft.world.entity.player.Player victim && !attacker.canHarmPlayer(victim)) return false;
		if (excludeAllies && owner != null && owner.isAlliedTo(candidate)) return false;
		return extra == null || extra.test(candidate);
	}
}
