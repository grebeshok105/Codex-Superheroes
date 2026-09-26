package io.github.grebeshok105.codex.ability.ironman;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.sound.ModSounds;
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
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		int current = player.getAttachedOrCreate(ModAttachments.SUIT_VARIANT);
		int next = IronManSuitVariant.nextIndex(current);
		player.setAttached(ModAttachments.SUIT_VARIANT, next);
		IronManSuitSyncController.broadcast(player);
		IronManSuitStats.apply(player);

		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
		// entity overload → ClientboundSoundEntityPacket: голос ДЖАРВИСа летит вместе
		// с Железным человеком, а не висит в точке активации
		level.playSound(null, player,
				ModSounds.IRONMAN_JARVIS_DIAGNOSTIC, SoundSource.PLAYERS, 0.8f, 1.0f);

		// [JARVIS] сообщение о смене костюма в чат убрано по запросу.

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
