package com.example.superheroes.item;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RegulusMadnessController;
import com.example.superheroes.effect.RegulusMadnessState;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.transform.HeroData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EvangelionItem extends Item {
	public EvangelionItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.GOLD);
		tooltip.add(TooltipFrame.flavor("item.superheroes.evangelion.lore.line1", ChatFormatting.GOLD));
		tooltip.add(TooltipFrame.flavor("item.superheroes.evangelion.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.evangelion.lore.usage", ChatFormatting.YELLOW));
		tooltip.add(TooltipFrame.bulletWarn("item.superheroes.evangelion.lore.warning", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.GOLD);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResultHolder.consume(stack);
		}
		HeroData data = sp.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !RegulusHero.ID.equals(data.heroId())) {
			sp.displayClientMessage(Component.translatable("item.superheroes.evangelion.not_regulus")
					.withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(stack);
		}
		RegulusMadnessState state = sp.getAttachedOrCreate(ModAttachments.REGULUS_MADNESS);
		if (state.madness() || state.isReading()) {
			sp.displayClientMessage(Component.translatable("item.superheroes.evangelion.already")
					.withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(stack);
		}
		RegulusMadnessController.startReading(sp);
		return InteractionResultHolder.consume(stack);
	}
}
