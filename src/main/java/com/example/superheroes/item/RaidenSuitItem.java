package com.example.superheroes.item;

import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * «Видение Электро» — артефакт, превращающий игрока в Райден Сёгун.
 * ПКМ — трансформация, Shift+ПКМ — отмена.
 */
public class RaidenSuitItem extends TransformationItem {
	public RaidenSuitItem(Properties properties) {
		super(RaidenHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.LIGHT_PURPLE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.raiden_suit.lore.line1", ChatFormatting.LIGHT_PURPLE));
		tooltip.add(TooltipFrame.flavor("item.superheroes.raiden_suit.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.raiden_suit.lore.usage", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.bullet("item.superheroes.raiden_suit.lore.untransform", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.LIGHT_PURPLE);
	}
}
