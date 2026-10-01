package io.github.grebeshok105.codex.hero.regulus.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.transform.TooltipFrame;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Evangelion book: a 60-tick vanilla item-use channel that opens Regulus's
 * madness ritual. {@link #use} starts the channel on BOTH sides — the client
 * must {@code startUsingItem} itself or the early-release path never reaches
 * {@link #releaseUsing}. Completion is vanilla ({@code updateUsingItem} →
 * {@link #finishUsingItem}); every earlier exit routes through
 * {@code releaseUsingItem} → {@link #releaseUsing} →
 * {@link RegulusMadnessController#interruptRitual}.
 */
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
		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}
		if (player instanceof ServerPlayer sp) {
			HeroData data = sp.getAttachedOrCreate(CoreAttachments.HERO_DATA);
			if (!data.hasHero() || !ModId.of("regulus").equals(data.heroId())) {
				sp.displayClientMessage(Component.translatable("item.superheroes.evangelion.not_regulus")
						.withStyle(ChatFormatting.GRAY), true);
				return InteractionResultHolder.fail(stack);
			}
			RegulusMadnessState state = sp.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
			if (state.madness() || state.isReading(sp.level().getGameTime())) {
				sp.displayClientMessage(Component.translatable("item.superheroes.evangelion.already")
						.withStyle(ChatFormatting.GRAY), true);
				return InteractionResultHolder.fail(stack);
			}
			RegulusMadnessController.beginReading(sp);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return RegulusMadnessController.RITUAL_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		if (!level.isClientSide && entity instanceof ServerPlayer sp
				&& RegulusMadnessController.ritualMovedTooFar(sp)) {
			sp.releaseUsingItem();
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide && entity instanceof ServerPlayer sp) {
			RegulusMadnessController.completeRitual(sp);
		}
		return stack;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!level.isClientSide && entity instanceof ServerPlayer sp) {
			RegulusMadnessController.interruptRitual(sp);
		}
	}
}
