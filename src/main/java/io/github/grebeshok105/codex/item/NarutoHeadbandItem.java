package io.github.grebeshok105.codex.item;

import io.github.grebeshok105.codex.hero.NarutoHero;
import io.github.grebeshok105.codex.transform.TooltipFrame;
import io.github.grebeshok105.codex.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class NarutoHeadbandItem extends TransformationItem {
	public NarutoHeadbandItem(Properties properties) {
		super(NarutoHero.ID, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.YELLOW);
		tooltip.add(TooltipFrame.flavor("item.superheroes.naruto_headband.lore.line1", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.flavor("item.superheroes.naruto_headband.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.naruto_headband.lore.usage", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.bullet("item.superheroes.naruto_headband.lore.untransform", ChatFormatting.RED));
		TooltipFrame.containsStone(tooltip, io.github.grebeshok105.codex.item.infinity.InfinityStoneType.SPACE);
		TooltipFrame.closeDivider(tooltip, ChatFormatting.YELLOW);
	}
}
