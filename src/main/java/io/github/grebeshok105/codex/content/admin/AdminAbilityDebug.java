package io.github.grebeshok105.codex.content.admin;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.MobTargetDebug;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Admin-side view of the ability mob-targeting debug switch. The flag itself lives in
 * {@link MobTargetDebug} because hero modules read it and may not depend on {@code content}
 * (R18); this facade keeps the admin content module as the command-facing owner — the
 * {@code /superheroes debug mob-targets} branch and gametests call these methods.
 */
public final class AdminAbilityDebug {
	private AdminAbilityDebug() {
	}

	public static boolean playerOnlyAbilitiesTargetMobs() {
		return MobTargetDebug.playerOnlyAbilitiesTargetMobs();
	}

	/**
	 * Mob-targeting support is an {@link Ability} trait (R18: debug code must not name
	 * concrete heroes), resolved through the registry instead of a content id list.
	 */
	public static boolean supportsMobTargets(ResourceLocation abilityId) {
		return MobTargetDebug.supportsMobTargets(abilityId);
	}

	public static boolean canPlayerOnlyAbilityTargetMobs(ResourceLocation abilityId) {
		return MobTargetDebug.canPlayerOnlyAbilityTargetMobs(abilityId);
	}

	public static boolean canTargetMob(ServerPlayer player, ResourceLocation abilityId, Mob target) {
		return MobTargetDebug.canTargetMob(player, abilityId, target);
	}

	public static boolean setPlayerOnlyAbilitiesTargetMobs(boolean enabled) {
		return MobTargetDebug.setPlayerOnlyAbilitiesTargetMobs(enabled);
	}

	public static boolean togglePlayerOnlyAbilitiesTargetMobs() {
		return MobTargetDebug.togglePlayerOnlyAbilitiesTargetMobs();
	}
}
