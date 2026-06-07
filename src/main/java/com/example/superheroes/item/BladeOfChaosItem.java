package com.example.superheroes.item;

import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BladeOfChaosItem extends TransformationItem {
	public BladeOfChaosItem(Properties properties) {
		super(KratosHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.RED);
		tooltip.add(TooltipFrame.flavor("item.superheroes.blade_of_chaos.lore.line1", ChatFormatting.RED));
		tooltip.add(TooltipFrame.flavor("item.superheroes.blade_of_chaos.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.blade_of_chaos.lore.usage", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.bullet("item.superheroes.blade_of_chaos.lore.untransform", ChatFormatting.RED));
		TooltipFrame.containsStone(tooltip, com.example.superheroes.item.infinity.InfinityStoneType.POWER);
		TooltipFrame.closeDivider(tooltip, ChatFormatting.RED);
	}
}
