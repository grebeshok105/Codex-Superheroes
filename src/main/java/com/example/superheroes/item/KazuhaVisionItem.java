package com.example.superheroes.item;

import com.example.superheroes.hero.KazuhaHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class KazuhaVisionItem extends TransformationItem {
	public KazuhaVisionItem(Properties properties) {
		super(KazuhaHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.GREEN);
		tooltip.add(TooltipFrame.flavor("item.superheroes.kazuha_vision.lore.line1", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.flavor("item.superheroes.kazuha_vision.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.kazuha_vision.lore.usage", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.bullet("item.superheroes.kazuha_vision.lore.untransform", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.GREEN);
	}
}
