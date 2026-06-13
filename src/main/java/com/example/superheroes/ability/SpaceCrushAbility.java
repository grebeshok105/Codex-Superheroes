package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.effect.SpatialBindController;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Pandora — «Сжатие пространства» (Space Crush). Dimension-only: collapses the
 * space around every <b>rope-bound</b> victim, crushing and killing them on the
 * spot. Kills <b>only</b> on activation (no passive/continuous damage), exactly
 * as specced. Instant.
 */
public final class SpaceCrushAbility implements Ability {
	private static final int COOLDOWN_TICKS = 5 * 20;
	private static final float CRUSH_DAMAGE = 1000f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SPACE_CRUSH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 80f;
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
		int crushed = 0;
		for (ServerPlayer victim : MirrorDimensionController.trappedVictims(player)) {
			if (!SpatialBindController.isBound(victim)) {
				continue; // only bound victims can be crushed
			}
			crush(player, victim);
			crushed++;
		}
		if (crushed == 0) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.space_crush.no_bound").withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		player.displayClientMessage(
				Component.translatable("ability.superheroes.space_crush.crushed", crushed)
						.withStyle(ChatFormatting.DARK_RED), true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static void crush(ServerPlayer caster, ServerPlayer victim) {
		ServerLevel level = victim.serverLevel();
		double x = victim.getX();
		double y = victim.getY() + 1.0;
		double z = victim.getZ();
		// Implosion visuals + sound.
		level.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 120, 0.5, 1.0, 0.5, 0.02);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 60, 0.5, 0.8, 0.5, 0.02);
		level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4f, 0.6f);
		level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0f, 0.5f);
		// Lethal blow credited to Pandora via the custom «space_crush» death type
		// (so the chat death message reads as a black-hole collapse, not «умер»);
		// release the ropes afterwards.
		victim.hurt(ModDamageTypes.spaceCrush(level, caster), CRUSH_DAMAGE);
		SpatialBindController.release(victim.getUUID());
	}
}
