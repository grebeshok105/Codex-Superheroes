package com.example.superheroes.item;

import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BattleBeastMedallionItem extends TransformationItem {
	public BattleBeastMedallionItem(Properties properties) {
		super(BattleBeastHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.DARK_RED);
		tooltip.add(TooltipFrame.flavor("item.superheroes.battle_beast_medallion.lore.line1", ChatFormatting.RED));
		tooltip.add(TooltipFrame.flavor("item.superheroes.battle_beast_medallion.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.battle_beast_medallion.lore.usage", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.bullet("item.superheroes.battle_beast_medallion.lore.untransform", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.DARK_RED);
	}
}
