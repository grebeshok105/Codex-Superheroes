package com.example.superheroes.item;

import com.example.superheroes.hero.InvincibleHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class InvincibleSuitItem extends TransformationItem {
	public InvincibleSuitItem(Properties properties) {
		super(InvincibleHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.BLUE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.invincible_suit.lore.line1", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.flavor("item.superheroes.invincible_suit.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.invincible_suit.lore.usage", ChatFormatting.AQUA));
		tooltip.add(TooltipFrame.bullet("item.superheroes.invincible_suit.lore.untransform", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.BLUE);
	}
}
