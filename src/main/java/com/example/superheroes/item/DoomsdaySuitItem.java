package com.example.superheroes.item;

import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DoomsdaySuitItem extends TransformationItem {
	public DoomsdaySuitItem(Properties properties) {
		super(DoomsdayHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.DARK_PURPLE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.doomsday_genome.lore.line1", ChatFormatting.LIGHT_PURPLE));
		tooltip.add(TooltipFrame.flavor("item.superheroes.doomsday_genome.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.doomsday_genome.lore.usage", ChatFormatting.RED));
		tooltip.add(TooltipFrame.bullet("item.superheroes.doomsday_genome.lore.untransform", ChatFormatting.GOLD));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.DARK_PURPLE);
	}
}
