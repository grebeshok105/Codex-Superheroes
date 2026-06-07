package com.example.superheroes.item;

import com.example.superheroes.hero.CaptainAmericaHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CaptainAmericaSuitItem extends TransformationItem {
	public CaptainAmericaSuitItem(Properties properties) {
		super(CaptainAmericaHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.BLUE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.captain_america_suit.lore.line1", ChatFormatting.BLUE));
		tooltip.add(TooltipFrame.flavor("item.superheroes.captain_america_suit.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.captain_america_suit.lore.usage", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.bullet("item.superheroes.captain_america_suit.lore.untransform", ChatFormatting.RED));
		TooltipFrame.containsStone(tooltip, com.example.superheroes.item.infinity.InfinityStoneType.SOUL);
		TooltipFrame.closeDivider(tooltip, ChatFormatting.BLUE);
	}
}
