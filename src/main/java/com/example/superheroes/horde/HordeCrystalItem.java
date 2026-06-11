package com.example.superheroes.horde;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class HordeCrystalItem extends Item {
	public HordeCrystalItem() {
		super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResultHolder.success(stack);
		}
		ServerLevel sl = (ServerLevel) level;
		if (HordeManager.hasActiveHorde(sl)) {
			player.sendSystemMessage(Component.literal("§cОрда уже активна в этом мире!"));
			return InteractionResultHolder.fail(stack);
		}
		HordeManager.startHorde(sl, player.position(), (ServerPlayer) player);
		stack.shrink(1);
		return InteractionResultHolder.consume(stack);
	}
}
