package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.item.MusouNoHitotachiItem;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * E — «Глаз Грозного Суда» (Eye of Stormy Judgment).
 * Тоггл на 25 секунд: каждый удар Yamato триггерит электро-проц + AoE.
 * Стоимость 50 энергии при активации, 0 во время действия (таймер заканчивает сам).
 */
public final class RaidenEyeOfJudgmentAbility implements Ability {
	public static final int DURATION_TICKS = 25 * 20;
	private static final float COST_ON_ACTIVATE = 50f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_EYE_OF_JUDGMENT;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return COST_ON_ACTIVATE;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		boolean hasYamato = main.getItem() instanceof MusouNoHitotachiItem || off.getItem() instanceof MusouNoHitotachiItem;
		if (!hasYamato) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_eye_of_judgment.no_sword"), true);
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		long now = player.serverLevel().getGameTime();
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		long expireAt = now + DURATION_TICKS;
		player.setAttached(ModAttachments.RAIDEN_STATE, state.withEyeExpireTick(expireAt));

		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.5, player.getZ(),
				40, 0.6, 0.8, 0.6, 0.25);
		level.sendParticles(ModParticles.FULA_PARTICLE,
				player.getX(), player.getY() + 1.0, player.getZ(),
				16, 0.4, 0.6, 0.4, 0.05);
		level.sendParticles(ModParticles.BLUE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.6, 0.4, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.4f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		long now = player.serverLevel().getGameTime();
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		if (state.eyeExpireTick() <= now) {
			com.example.superheroes.ability.AbilityRouter.deactivate(player, getId());
			return;
		}
		if (now % 6 == 0) {
			ServerLevel level = player.serverLevel();
			level.sendParticles(ModParticles.JIWALD_EFFECT,
					player.getX(), player.getY() + 1.0, player.getZ(),
					3, 0.4, 0.5, 0.4, 0.05);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		player.setAttached(ModAttachments.RAIDEN_STATE, state.withEyeExpireTick(0L));
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.BLUE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.6, 0.4, 0.02);
	}
}
