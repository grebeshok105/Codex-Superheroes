package com.example.superheroes.ability;

import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.effect.VanityAuthority;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Pandora — «Лишение тщеславием» (Vanity Strip). Dimension-only TOGGLE.
 *
 * <p>A dedicated ability (not automatic): while toggled on inside the House of
 * Vanity, every trapped victim loses ALL of their powers — active abilities and
 * passives, from this mod AND from the friend's mod (falbiks_heroes), for every
 * hero present or future — while keeping their skin and identity. Toggle it off
 * (or the victim escapes the House) and their powers return within a moment.
 */
public final class VanityStripAbility implements Ability {
	private static final int COOLDOWN_TICKS = 2 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.VANITY_STRIP;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 50f;
	}

	@Override
	public float costPerTick() {
		return 0.3f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return MirrorDimensionController.hasActiveHouse(player)
				&& !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		// Strip immediately on activation so it feels instant, then keep it up via onTickActive.
		stripAll(player);
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8f, 1.4f);
		player.displayClientMessage(
				Component.translatable("ability.superheroes.vanity_strip.on").withStyle(ChatFormatting.DARK_PURPLE), true);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		// Refresh the strip on everyone currently trapped (covers latecomers too).
		stripAll(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		// Nothing to undo: the VANITY_STRIPPED marker is a short refreshable effect and
		// expires on its own within ~3s once we stop refreshing it, restoring all powers.
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		player.displayClientMessage(
				Component.translatable("ability.superheroes.vanity_strip.off").withStyle(ChatFormatting.GRAY), true);
	}

	private static void stripAll(ServerPlayer pandora) {
		for (ServerPlayer victim : MirrorDimensionController.trappedVictims(pandora)) {
			VanityAuthority.applyStrip(victim);
		}
	}
}
