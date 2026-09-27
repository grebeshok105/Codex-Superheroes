package io.github.grebeshok105.codex.hero.raiden.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenState;
import io.github.grebeshok105.codex.particle.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Transcendence: Baleful Omen — toggle. Включает пассивную ауру: каждые 30 тиков
 * выстреливает молнией в ближайшего врага в радиусе 6. Стоит 0.6 энергии в тик.
 * Логика тикания — в {@link io.github.grebeshok105.codex.hero.raiden.runtime.RaidenAuraController}.
 */
public final class RaidenTranscendenceAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("raiden_transcendence");

	@Override
	public ResourceLocation getId() {
		return ID;
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
		RaidenState state = player.getAttachedOrCreate(RaidenState.ATTACHMENT);
		player.setAttached(RaidenState.ATTACHMENT,
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
		RaidenState state = player.getAttachedOrCreate(RaidenState.ATTACHMENT);
		player.setAttached(RaidenState.ATTACHMENT, state.withTranscendenceUntilTick(0L));
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.5, 0.4, 0.05);
	}
}
