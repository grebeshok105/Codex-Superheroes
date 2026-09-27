package io.github.grebeshok105.codex.hero.reinhard.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardState;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ReinhardCounterRiposteAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("reinhard_counter_riposte");

	public static final int RIPOSTE_DURATION_TICKS = 40;
	private static final int COOLDOWN_TICKS = 240;

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
		return 200f;
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
		long now = level.getGameTime();
		ReinhardState state = player.getAttachedOrCreate(ReinhardState.ATTACHMENT);
		player.setAttached(ReinhardState.ATTACHMENT,
				state.withRiposteExpireTick(now + RIPOSTE_DURATION_TICKS));

		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, RIPOSTE_DURATION_TICKS, 0, true, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RIPOSTE_DURATION_TICKS, 1, true, false, false));

		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.2, player.getZ(),
				36, 0.6, 0.4, 0.6, 0.04);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				player.getX(), player.getY() + 1.2, player.getZ(),
				18, 0.4, 0.4, 0.4, 0.02);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 1.4f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.6f);
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_counter_riposte.armed"),
				true);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
