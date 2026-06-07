package com.example.superheroes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VibraniumShieldItem extends ShieldItem {
	public VibraniumShieldItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.BLUE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.vibranium_shield.lore.line1", ChatFormatting.BLUE));
		tooltip.add(TooltipFrame.flavor("item.superheroes.vibranium_shield.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.vibranium_shield.lore.usage_block", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.bullet("item.superheroes.vibranium_shield.lore.usage_throw", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.BLUE);
	}
}
