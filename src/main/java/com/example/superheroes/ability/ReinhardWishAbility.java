package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.network.ReinhardWishOptionsS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

public final class ReinhardWishAbility implements Ability {
	public static final int MAX_WISHES = 3;
	private static final int COOLDOWN_TICKS = 600;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_WISH;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		ReinhardState s = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		ServerLevel level = player.serverLevel();
		long now = level.getGameTime();
		if (s.lastWishTick() != 0 && now - s.lastWishTick() < COOLDOWN_TICKS) return false;
		if (s.wishesUsed() >= MAX_WISHES) return false;
		if (s.recentDamageTypes().isEmpty()) return false;
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ReinhardState s = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		List<String> recent = s.recentDamageTypes();
		if (recent.isEmpty()) return false;

		ServerPlayNetworking.send(player, new ReinhardWishOptionsS2CPayload(
				List.copyOf(recent),
				List.copyOf(s.adaptedDamageTypes()),
				s.wishesUsed(),
				MAX_WISHES
		));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.8f);
		return true;
	}

	public static void confirm(ServerPlayer player, String damageTypeId) {
		ReinhardState s = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		ServerLevel level = player.serverLevel();
		long now = level.getGameTime();

		if (s.lastWishTick() != 0 && now - s.lastWishTick() < COOLDOWN_TICKS) return;
		if (s.wishesUsed() >= MAX_WISHES) return;
		if (damageTypeId == null || damageTypeId.isEmpty()) return;
		if (!s.recentDamageTypes().contains(damageTypeId)) return;
		if (s.adaptedDamageTypes().contains(damageTypeId)) return;

		List<String> adapted = new ArrayList<>(s.adaptedDamageTypes());
		adapted.add(damageTypeId);

		ReinhardState updated = s.withAdaptedDamageTypes(adapted)
				.withWishesUsed(s.wishesUsed() + 1)
				.withLastWishTick(now);
		player.setAttached(ModAttachments.REINHARD_STATE, updated);

		level.sendParticles(ParticleTypes.GLOW,
				player.getX(), player.getY() + 1.0, player.getZ(),
				50, 0.6, 1.0, 0.6, 0.05);
		level.sendParticles(ParticleTypes.END_ROD,
				player.getX(), player.getY() + 1.0, player.getZ(),
				24, 0.3, 0.6, 0.3, 0.1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.4f);

		String label = damageTypeId.startsWith("minecraft:") ? damageTypeId.substring("minecraft:".length()) : damageTypeId;
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_wish.granted", label),
				true);
	}
}
