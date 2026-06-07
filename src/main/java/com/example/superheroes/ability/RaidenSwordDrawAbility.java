package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Manifest Yamato — toggle. Призывает фиолетовую Ямато в руку.
 * Без активной Manifest Yamato меч полностью отсутствует у Райден.
 * Деактивация — Ямато исчезает (рассыпается на молнии).
 */
public final class RaidenSwordDrawAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_SWORD_DRAW;
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
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		player.setAttached(ModAttachments.RAIDEN_STATE, state.withSwordDrawn(true));
		giveSword(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.SWORD_EXPLOSION,
				player.getX(), player.getY() + 1.0, player.getZ(),
				24, 0.4, 0.7, 0.4, 0.18);
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.5, 0.7, 0.5, 0.25);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);
		return true;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
		player.setAttached(ModAttachments.RAIDEN_STATE, state.withSwordDrawn(false));
		removeSword(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.BLUE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				18, 0.4, 0.6, 0.4, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7f, 1.3f);
	}

	public static void giveSword(ServerPlayer player) {
		if (player.getMainHandItem().is(ModItems.MUSOU_NO_HITOTACHI)) return;
		if (player.getOffhandItem().is(ModItems.MUSOU_NO_HITOTACHI)) return;
		ItemStack stack = new ItemStack(ModItems.MUSOU_NO_HITOTACHI);
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		} else if (player.getOffhandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.OFF_HAND, stack);
		} else {
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
	}

	public static void removeSword(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(ModItems.MUSOU_NO_HITOTACHI)) {
				player.getInventory().setItem(i, ItemStack.EMPTY);
			}
		}
	}
}
