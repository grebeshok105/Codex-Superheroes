package com.example.superheroes.item;

import com.example.superheroes.hero.OmnimanHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OmnimanSuitItem extends TransformationItem {
	public OmnimanSuitItem(Properties properties) {
		super(OmnimanHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.DARK_RED);
		tooltip.add(TooltipFrame.flavor("item.superheroes.omniman_suit.lore.line1", ChatFormatting.RED));
		tooltip.add(TooltipFrame.flavor("item.superheroes.omniman_suit.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.omniman_suit.lore.usage", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.bullet("item.superheroes.omniman_suit.lore.untransform", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.DARK_RED);
	}
}
