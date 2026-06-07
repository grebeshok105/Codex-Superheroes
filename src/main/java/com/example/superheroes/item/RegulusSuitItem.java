package com.example.superheroes.item;

import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RegulusSuitItem extends TransformationItem {
	public RegulusSuitItem(Properties properties) {
		super(RegulusHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.GOLD);
		tooltip.add(TooltipFrame.flavor("item.superheroes.regulus_suit.lore.line1", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.flavor("item.superheroes.regulus_suit.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.regulus_suit.lore.usage", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.bullet("item.superheroes.regulus_suit.lore.untransform", ChatFormatting.GOLD));
		TooltipFrame.containsStone(tooltip, com.example.superheroes.item.infinity.InfinityStoneType.TIME);
		TooltipFrame.closeDivider(tooltip, ChatFormatting.GOLD);
	}
}
