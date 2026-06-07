package com.example.superheroes.ability;

import com.example.superheroes.effect.DoomGripController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class DoomGripAbility implements Ability {
	private static final int COOLDOWN_TICKS = 900; // 45s
	private static final double SEARCH_RADIUS = 8.0;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.DOOMSDAY_DOOM_GRIP;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 100f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		AABB box = new AABB(origin.subtract(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS),
				origin.add(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS));
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !isAlly(e, player));
		LivingEntity target = candidates.stream()
				.min(Comparator.comparingDouble(e -> e.position().distanceToSqr(origin)))
				.orElse(null);
		if (target == null) {
			AbilityCooldowns.setCooldownTicks(player, getId(), 100);
			return false;
		}
		DoomGripController.start(player, target);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static boolean isAlly(LivingEntity e, Player player) {
		return e.getUUID().equals(player.getUUID());
	}
}
