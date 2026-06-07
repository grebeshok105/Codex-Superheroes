package com.example.superheroes.item.infinity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class InfinityStoneItem extends Item {
	private final InfinityStoneType stoneType;

	public InfinityStoneItem(InfinityStoneType stoneType, Properties properties) {
		super(properties);
		this.stoneType = stoneType;
	}

	public InfinityStoneType getStoneType() {
		return stoneType;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		String base = "item.superheroes." + stoneType.getItemRegistryName();
		tooltip.add(Component.translatable(base + ".desc").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(base + ".bonus").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.empty());
		tooltip.add(Component.translatable("item.superheroes.infinity_stone.usage").withStyle(ChatFormatting.GOLD));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
