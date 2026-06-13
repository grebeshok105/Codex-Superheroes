package com.example.superheroes.ability;

import com.example.superheroes.effect.MirrorDimensionController;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Doctor Strange — Distortion Switch. Cycles the active Mirror Dimension warp
 * through MODE 4 -> 5 -> 6 -> 9 (with matching J scales). Each switch needs a
 * full Iris pipeline reload on the victim, so the victim's client hides the
 * freeze behind a black flash. Does nothing if no Mirror Dimension is active.
 */
public final class MirrorModeCycleAbility implements Ability {
	private static final int COOLDOWN_TICKS = 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.MIRROR_MODE_CYCLE;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 5f;
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
		if (!MirrorDimensionController.cycleMode(player)) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_mode_cycle.no_session").withStyle(ChatFormatting.RED), true);
			return false;
		}
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
