package io.github.grebeshok105.codex.core.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Cross-hero activation rules owned by whoever declares the state (Snap, Vanity strip, madness). Registration order is evaluation order. */
public final class AbilityRules {
	private static final List<AbilityBlocker> BLOCKERS = new ArrayList<>();
	private static final List<Predicate<ServerPlayer>> FREE_COST = new ArrayList<>();
	private static final List<Predicate<ServerPlayer>> HIDE_ALL = new ArrayList<>();

	private AbilityRules() {
	}

	/** Checked first, before hero membership — states that forbid every ability. */
	public static void blocker(AbilityBlocker blocker) {
		BLOCKERS.add(blocker);
	}

	public static void freeCost(Predicate<ServerPlayer> rule) {
		FREE_COST.add(rule);
	}

	/** States that hide the victim's whole ability list in {@code AbilityAvailabilitySync} (Vanity strip, …). */
	public static void hideAll(Predicate<ServerPlayer> rule) {
		HIDE_ALL.add(rule);
	}

	@Nullable
	public static AbilityDenial firstBlock(ServerPlayer player, ResourceLocation abilityId) {
		for (AbilityBlocker blocker : BLOCKERS) {
			AbilityDenial denial = blocker.check(player, abilityId);
			if (denial != null) {
				return denial;
			}
		}
		return null;
	}

	public static boolean isFree(ServerPlayer player) {
		for (Predicate<ServerPlayer> rule : FREE_COST) {
			if (rule.test(player)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hidesAll(ServerPlayer player) {
		for (Predicate<ServerPlayer> rule : HIDE_ALL) {
			if (rule.test(player)) {
				return true;
			}
		}
		return false;
	}
}
