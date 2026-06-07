package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;

public final class HeroEquipmentLock {
	private HeroEquipmentLock() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				stripIfHero(player);
			}
		});
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!isLockedItem(stack)) {
				return InteractionResultHolder.pass(stack);
			}
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.hasHero()) {
				return InteractionResultHolder.pass(stack);
			}
			if (!world.isClientSide) {
				player.displayClientMessage(Component.translatable("message.superheroes.armor_locked"), true);
			}
			return InteractionResultHolder.fail(stack);
		});
	}

	private static void stripIfHero(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Inventory inv = player.getInventory();
		stripList(player, inv.armor);
	}

	private static void stripList(ServerPlayer player, NonNullList<ItemStack> slots) {
		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i);
			if (stack.isEmpty() || !isLockedItem(stack)) {
				continue;
			}
			slots.set(i, ItemStack.EMPTY);
			ItemStack remainder = stack.copy();
			if (!player.getInventory().add(remainder)) {
				player.drop(remainder, false);
			}
		}
	}

	private static boolean isLockedItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.getItem() instanceof ArmorItem) {
			return true;
		}
		if (stack.getItem() instanceof ElytraItem) {
			return true;
		}
		return false;
	}
}
