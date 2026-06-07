package com.example.superheroes.ability;

import com.example.superheroes.entity.ShieldProjectileEntity;
import com.example.superheroes.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class CapShieldThrowAbility implements Ability {
	private static final int COOLDOWN_TICKS = 100;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.CAP_SHIELD_THROW;
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
		return !AbilityCooldowns.isOnCooldown(player, getId()) && findShieldHand(player) != null;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		InteractionHand hand = findShieldHand(player);
		if (hand == null) return false;
		ServerLevel level = player.serverLevel();
		ItemStack shieldStack = player.getItemInHand(hand).copy();
		if (player.isUsingItem() && player.getUsedItemHand() == hand) {
			player.stopUsingItem();
		}
		player.setItemInHand(hand, ItemStack.EMPTY);
		ShieldProjectileEntity projectile = ShieldProjectileEntity.throwFrom(player, level, shieldStack, hand);
		level.addFreshEntity(projectile);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.4f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	private static InteractionHand findShieldHand(ServerPlayer player) {
		if (player.getOffhandItem().is(ModItems.VIBRANIUM_SHIELD)) return InteractionHand.OFF_HAND;
		if (player.getMainHandItem().is(ModItems.VIBRANIUM_SHIELD)) return InteractionHand.MAIN_HAND;
		return null;
	}
}
