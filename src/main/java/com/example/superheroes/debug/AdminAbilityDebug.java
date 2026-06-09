package com.example.superheroes.debug;

import com.example.superheroes.ability.AbilityIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.Set;

public final class AdminAbilityDebug {
	private static final Set<ResourceLocation> MOB_TARGET_ABILITIES = Set.of(AbilityIds.REINHARD_SPEED_JUDGMENT);
	private static volatile boolean playerOnlyAbilitiesTargetMobs;

	private AdminAbilityDebug() {
	}

	public static boolean playerOnlyAbilitiesTargetMobs() {
		return playerOnlyAbilitiesTargetMobs;
	}

	public static boolean supportsMobTargets(ResourceLocation abilityId) {
		return MOB_TARGET_ABILITIES.contains(abilityId);
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
