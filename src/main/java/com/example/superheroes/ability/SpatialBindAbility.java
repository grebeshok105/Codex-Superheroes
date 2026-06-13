package com.example.superheroes.ability;

import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.effect.SpatialBindController;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Pandora — «Пространственная привязка» (Spatial Bind). Dimension-only: while
 * her House of Vanity is open, this nails every trapped victim in place with
 * glowing spatial ropes anchored into the sky and ground, fully rooting them
 * (see {@link SpatialBindController}). Bound victims can then be finished with
 * {@link SpaceCrushAbility}. Instant (not a toggle).
 */
public final class SpatialBindAbility implements Ability {
	private static final int COOLDOWN_TICKS = 3 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SPATIAL_BIND;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 40f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return MirrorDimensionController.hasActiveHouse(player)
				&& !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		var victims = MirrorDimensionController.trappedVictims(player);
		if (victims.isEmpty()) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.spatial_bind.no_targets").withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		for (ServerPlayer victim : victims) {
			SpatialBindController.bind(player, victim);
			ServerLevel level = victim.serverLevel();
			level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
					SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.2f, 0.7f);
		}
		player.displayClientMessage(
				Component.translatable("ability.superheroes.spatial_bind.bound", victims.size())
						.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
