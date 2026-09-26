package io.github.grebeshok105.codex.debug;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

public final class AdminAbilityDebug {
	private static volatile boolean playerOnlyAbilitiesTargetMobs;

	private AdminAbilityDebug() {
	}

	public static boolean playerOnlyAbilitiesTargetMobs() {
		return playerOnlyAbilitiesTargetMobs;
	}

	/**
	 * Mob-targeting support is an {@link Ability} trait now (R18: debug code must not name
	 * concrete heroes), resolved through the registry instead of a content id list.
	 */
	public static boolean supportsMobTargets(ResourceLocation abilityId) {
		Ability ability = AbilityRegistry.get(abilityId);
		return ability != null && ability.debugTargetsMobs();
	}

	public static boolean canPlayerOnlyAbilityTargetMobs(ResourceLocation abilityId) {
		return playerOnlyAbilitiesTargetMobs && supportsMobTargets(abilityId);
	}

	public static boolean canTargetMob(ServerPlayer player, ResourceLocation abilityId, Mob target) {
		return player != null
				&& target != null
				&& canPlayerOnlyAbilityTargetMobs(abilityId)
				&& target.isAlive()
				&& !target.isRemoved()
				&& target.level() == player.serverLevel();
	}

	public static boolean setPlayerOnlyAbilitiesTargetMobs(boolean enabled) {
		playerOnlyAbilitiesTargetMobs = enabled;
		return playerOnlyAbilitiesTargetMobs;
	}

	public static boolean togglePlayerOnlyAbilitiesTargetMobs() {
		playerOnlyAbilitiesTargetMobs = !playerOnlyAbilitiesTargetMobs;
		return playerOnlyAbilitiesTargetMobs;
	}
}
