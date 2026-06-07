package com.example.superheroes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class UraniumIsotopeItem extends Item {
	public UraniumIsotopeItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.GREEN);
		tooltip.add(TooltipFrame.flavor("item.superheroes.uranium_isotope.lore.line1", ChatFormatting.GREEN));
		tooltip.add(TooltipFrame.flavor("item.superheroes.uranium_isotope.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.uranium_isotope.lore.usage", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.bullet("item.superheroes.uranium_isotope.lore.offhand", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.bullet("item.superheroes.uranium_isotope.lore.radiation", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.GREEN);
	}
}
