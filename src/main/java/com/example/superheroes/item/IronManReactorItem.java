package com.example.superheroes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class IronManReactorItem extends Item {
	public IronManReactorItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.AQUA);
		tooltip.add(TooltipFrame.flavor("item.superheroes.iron_man_reactor.lore.line1", ChatFormatting.AQUA));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.iron_man_reactor.lore.usage", ChatFormatting.GOLD));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.AQUA);
	}
}
