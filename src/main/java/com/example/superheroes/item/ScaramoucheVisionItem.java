package com.example.superheroes.item;

import com.example.superheroes.hero.ScaramoucheHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ScaramoucheVisionItem extends TransformationItem {
	public ScaramoucheVisionItem(Properties properties) {
		super(ScaramoucheHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.DARK_AQUA);
		tooltip.add(TooltipFrame.flavor("item.superheroes.scaramouche_vision.lore.line1", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.flavor("item.superheroes.scaramouche_vision.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.scaramouche_vision.lore.usage", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.bullet("item.superheroes.scaramouche_vision.lore.untransform", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.DARK_AQUA);
	}
}
