package com.example.superheroes.item;

import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DoctorStrangeSuitItem extends TransformationItem {
	public DoctorStrangeSuitItem(Properties properties) {
		super(PandoraHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.DARK_RED);
		tooltip.add(TooltipFrame.flavor("item.superheroes.doctor_strange_suit.lore.line1", ChatFormatting.RED));
		tooltip.add(TooltipFrame.flavor("item.superheroes.doctor_strange_suit.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.doctor_strange_suit.lore.usage", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.bullet("item.superheroes.doctor_strange_suit.lore.untransform", ChatFormatting.GOLD));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.DARK_RED);
	}
}
