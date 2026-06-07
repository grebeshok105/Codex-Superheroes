package com.example.superheroes.item;

import com.example.superheroes.hero.SungJinwooHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ShadowMonarchsCloakItem extends TransformationItem {
	public ShadowMonarchsCloakItem(Properties properties) {
		super(SungJinwooHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.LIGHT_PURPLE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.shadow_monarchs_cloak.lore.line1", ChatFormatting.LIGHT_PURPLE));
		tooltip.add(TooltipFrame.flavor("item.superheroes.shadow_monarchs_cloak.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.shadow_monarchs_cloak.lore.usage", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.bullet("item.superheroes.shadow_monarchs_cloak.lore.untransform", ChatFormatting.LIGHT_PURPLE));
		TooltipFrame.containsStone(tooltip, com.example.superheroes.item.infinity.InfinityStoneType.REALITY);
		TooltipFrame.closeDivider(tooltip, ChatFormatting.LIGHT_PURPLE);
	}
}
