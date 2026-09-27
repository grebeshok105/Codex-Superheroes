package io.github.grebeshok105.codex.hero.ironman.ability;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.hero.ironman.IronManSuitVariant;
import io.github.grebeshok105.codex.hero.ironman.sound.IronManSounds;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManSuitStats;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManSuitSyncController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public final class IronManSuitSwitchAbility implements Ability {
	private static final int COOLDOWN_TICKS = 40;

	public static final ResourceLocation ID = ModId.of("iron_man_suit_switch");

	@Override
	public ResourceLocation getId() {
		return ID;
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
		int current = player.getAttachedOrCreate(IronManSuitVariant.ATTACHMENT);
		int next = IronManSuitVariant.nextIndex(current);
		player.setAttached(IronManSuitVariant.ATTACHMENT, next);
		IronManSuitSyncController.broadcast(player);
		IronManSuitStats.apply(player);

		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
		// entity overload → ClientboundSoundEntityPacket: голос ДЖАРВИСа летит вместе
		// с Железным человеком, а не висит в точке активации
		level.playSound(null, player,
				IronManSounds.IRONMAN_JARVIS_DIAGNOSTIC, SoundSource.PLAYERS, 0.8f, 1.0f);

		// [JARVIS] сообщение о смене костюма в чат убрано по запросу.

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
