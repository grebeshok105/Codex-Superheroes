package com.example.superheroes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.UUID;

public class KryptoniteShardItem extends Item {
	public static final String KEY_OWNER = "OwnerUUID";
	public static final String KEY_TARGET = "TargetDoomsdayUUID";

	public KryptoniteShardItem(Properties properties) {
		super(properties);
	}

	public static ItemStack create(UUID owner, UUID targetDoomsday) {
		ItemStack stack = new ItemStack(com.example.superheroes.item.ModItems.KRYPTONITE_SHARD);
		CompoundTag tag = new CompoundTag();
		tag.putUUID(KEY_OWNER, owner);
		tag.putUUID(KEY_TARGET, targetDoomsday);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	public static UUID getOwner(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return null;
		CompoundTag tag = data.copyTag();
		if (!tag.hasUUID(KEY_OWNER)) return null;
		return tag.getUUID(KEY_OWNER);
	}

	public static UUID getTargetDoomsday(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return null;
		CompoundTag tag = data.copyTag();
		if (!tag.hasUUID(KEY_TARGET)) return null;
		return tag.getUUID(KEY_TARGET);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.superheroes.kryptonite_shard.lore.line1").withStyle(ChatFormatting.GREEN));
		tooltip.add(Component.translatable("item.superheroes.kryptonite_shard.lore.line2").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.superheroes.kryptonite_shard.lore.line3").withStyle(ChatFormatting.AQUA));
	}
}
