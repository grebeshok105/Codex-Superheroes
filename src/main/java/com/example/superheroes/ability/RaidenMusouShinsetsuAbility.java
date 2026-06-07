package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.hero.HeroAttributes;
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
 * Q — «Musou Shinsetsu» (Burst).
 * Запускает 7-секундный режим: +50% скорость, +1.5 attack speed, +6 attack damage,
 * каждый удар Yamato бьёт усиленно с AoE.
 * В конце окна — автоматический финальный AoE-слэш по всем врагам в радиусе 8.
 *
 * Фиксированный КД 25с. Стоимость 500 энергии gate (один раз).
 */
public final class RaidenMusouShinsetsuAbility implements Ability {
	public static final int DURATION_TICKS = 7 * 20;
	public static final int COOLDOWN_TICKS = 25 * 20;
	private static final float COST_ON_ACTIVATE = 500f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_MUSOU_SHINSETSU;
	}

	@Override
	public boolean isToggle() {
		return false;
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
					Component.translatable("ability.superheroes.raiden_musou_shinsetsu.no_sword"), true);
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		long now = player.serverLevel().getGameTime();
		long expireAt = now + DURATION_TICKS;
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		player.setAttached(ModAttachments.RAIDEN_STATE,
				state.withBurstExpireTick(expireAt).withBurstFinalSlashTick(expireAt));

		HeroAttributes.RAIDEN_BURST.apply(player);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);

		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.0, player.getZ(),
				120, 1.2, 1.6, 1.2, 0.6);
		level.sendParticles(ModParticles.SWORD_EXPLOSION,
				player.getX(), player.getY() + 1.5, player.getZ(),
				24, 0.8, 1.0, 0.8, 0.15);
		level.sendParticles(ModParticles.BLUE_FLAME,
				player.getX(), player.getY() + 1.5, player.getZ(),
				40, 0.8, 1.0, 0.8, 0.1);
		level.sendParticles(ModParticles.MOONVEIL,
				player.getX(), player.getY() + 1.5, player.getZ(),
				12, 0.6, 0.6, 0.6, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.6f);
		return true;
	}
}
