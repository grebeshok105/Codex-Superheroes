package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Transcendence: Baleful Omen — toggle. Включает пассивную ауру: каждые 30 тиков
 * выстреливает молнией в ближайшего врага в радиусе 6. Стоит 0.6 энергии в тик.
 * Логика тикания — в {@link com.example.superheroes.effect.RaidenAuraController}.
 */
public final class RaidenTranscendenceAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_TRANSCENDENCE;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 0.6f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		player.setAttached(ModAttachments.RAIDEN_STATE,
				state.withTranscendenceUntilTick(Long.MAX_VALUE));
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.BLUE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				24, 0.4, 0.6, 0.4, 0.04);
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.5, player.getZ(),
				14, 0.4, 0.4, 0.4, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		if (player.tickCount % 8 == 0) {
			ServerLevel level = player.serverLevel();
			level.sendParticles(ModParticles.BLUE_FLAME,
					player.getX(), player.getY() + 1.0, player.getZ(),
					2, 0.3, 0.4, 0.3, 0.02);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		player.setAttached(ModAttachments.RAIDEN_STATE, state.withTranscendenceUntilTick(0L));
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.5, 0.4, 0.05);
	}
}
