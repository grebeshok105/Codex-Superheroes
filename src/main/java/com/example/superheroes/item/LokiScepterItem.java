package com.example.superheroes.item;

import com.example.superheroes.hero.LokiHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LokiScepterItem extends TransformationItem {
	public LokiScepterItem(Properties properties) {
		super(LokiHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.GREEN);
		tooltip.add(TooltipFrame.flavor("item.superheroes.loki_scepter.lore.line1", ChatFormatting.GREEN));
		tooltip.add(TooltipFrame.flavor("item.superheroes.loki_scepter.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.loki_scepter.lore.usage", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.bullet("item.superheroes.loki_scepter.lore.untransform", ChatFormatting.RED));
		TooltipFrame.containsStone(tooltip, com.example.superheroes.item.infinity.InfinityStoneType.MIND);
		TooltipFrame.closeDivider(tooltip, ChatFormatting.GREEN);
	}
}
