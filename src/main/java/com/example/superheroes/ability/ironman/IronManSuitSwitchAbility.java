package com.example.superheroes.ability.ironman;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.jarvis.JarvisQuotes;
import com.example.superheroes.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public final class IronManSuitSwitchAbility implements Ability {
	private static final int COOLDOWN_TICKS = 40;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_SUIT_SWITCH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 50f;
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
		int current = player.getAttachedOrCreate(ModAttachments.SUIT_VARIANT);
		int next = IronManSuitVariant.nextIndex(current);
		player.setAttached(ModAttachments.SUIT_VARIANT, next);
		IronManSuitSyncController.broadcast(player);
		IronManSuitStats.apply(player);

		IronManSuitVariant variant = IronManSuitVariant.get(next);

		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.IRONMAN_JARVIS_DIAGNOSTIC, SoundSource.PLAYERS, 0.8f, 1.0f);

		String quote = JarvisQuotes.randomSuitSwitch();
		player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"§6[JARVIS] §f" + quote + " §7[" + variant.nameRu() + "]"));

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
