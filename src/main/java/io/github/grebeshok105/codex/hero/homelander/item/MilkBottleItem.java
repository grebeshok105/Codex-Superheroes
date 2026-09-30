package io.github.grebeshok105.codex.hero.homelander.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.core.transform.TooltipFrame;
import io.github.grebeshok105.codex.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class MilkBottleItem extends Item {
	private static final net.minecraft.resources.ResourceLocation HOMELANDER_ID = ModId.of("homelander");

	// The authored 6.3 s milk sequence owns the whole use bar; MADNESS lands
	// in finishUsingItem only when all 126 ticks elapse.
	private static final int DRINK_TICKS = 126;
	private static final int MADNESS_DURATION_TICKS = 15 * 20;

	public MilkBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.WHITE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.milk_bottle.lore.line1", ChatFormatting.WHITE));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.milk_bottle.lore.usage", ChatFormatting.RED));
		tooltip.add(TooltipFrame.bulletWarn("item.superheroes.milk_bottle.lore.warning", ChatFormatting.DARK_RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.WHITE);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return DRINK_TICKS;
	}

	@Override
	public SoundEvent getDrinkingSound() {
		// Silent so vanilla sipping doesn't stutter over the designed
		// one-shot the MILK_DRINK event plays.
		return ModSounds.SILENT;
	}

	@Override
	public SoundEvent getEatingSound() {
		return ModSounds.SILENT;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isHomelander(player)) {
			if (!level.isClientSide()) {
				player.displayClientMessage(
						Component.translatable("item.superheroes.milk_bottle.not_homelander")
								.withStyle(ChatFormatting.GRAY),
						true);
			}
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			MilkDrinkTracker.started(serverPlayer);
			VfxFx.event(player, HomelanderVfxIds.MILK_DRINK,
					player.position(), player.position(), 1f);
		}
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (level.isClientSide() || !(user instanceof ServerPlayer serverPlayer)) {
			return super.finishUsingItem(stack, level, user);
		}
		if (!isHomelander(serverPlayer)) {
			return stack;
		}
		MilkDrinkTracker.finished(serverPlayer);
		serverPlayer.addEffect(new MobEffectInstance(HomelanderEffects.MADNESS, MADNESS_DURATION_TICKS, 0, false, true, true));
		if (!serverPlayer.getAbilities().instabuild) {
			stack.shrink(1);
			ItemStack empty = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
			if (stack.isEmpty()) {
				return empty;
			}
			if (!serverPlayer.getInventory().add(empty)) {
				serverPlayer.drop(empty, false);
			}
		}
		return stack;
	}

	private static boolean isHomelander(Player player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.hasHero() && HOMELANDER_ID.equals(data.heroId());
	}
}
