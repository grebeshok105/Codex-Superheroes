package com.example.superheroes.item;

import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.infinity.InfinityGauntletData;
import com.example.superheroes.item.infinity.InfinityStoneItem;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.example.superheroes.transform.HeroTransformService;
import com.example.superheroes.transform.TransformationItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

public class InfinityGauntletItem extends TransformationItem {
	public InfinityGauntletItem(Properties properties) {
		super(ThanosHero.ID, properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack gauntlet = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResultHolder.success(gauntlet);
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.pass(gauntlet);
		}

		InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack other = player.getItemInHand(otherHand);

		if (player.isShiftKeyDown()) {
			InfinityStoneType ejected = InfinityGauntletData.ejectLast(gauntlet);
			if (ejected != null) {
				ItemStack stone = stoneStack(ejected);
				if (!player.getInventory().add(stone)) {
					player.drop(stone, false);
				}
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9f, 1.4f);
				return InteractionResultHolder.consume(gauntlet);
			}
			boolean changed = HeroTransformService.untransform(serverPlayer);
			return changed ? InteractionResultHolder.consume(gauntlet) : InteractionResultHolder.fail(gauntlet);
		}

		if (other.getItem() instanceof InfinityStoneItem stoneItem) {
			InfinityStoneType type = stoneItem.getStoneType();
			if (!InfinityGauntletData.hasStone(gauntlet, type) && !InfinityGauntletData.isFull(gauntlet)) {
				if (InfinityGauntletData.tryInsert(gauntlet, type)) {
					other.shrink(1);
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.8f);
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.4f, 0.6f);
					return InteractionResultHolder.consume(gauntlet);
				}
			}
		}

		boolean changed = HeroTransformService.transform(serverPlayer, ThanosHero.ID);
		return changed ? InteractionResultHolder.consume(gauntlet) : InteractionResultHolder.fail(gauntlet);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		List<InfinityStoneType> stones = InfinityGauntletData.getStones(stack);
		int count = stones.size();
		int max = InfinityStoneType.values().length;

		tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.desc").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.stones",
						Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD),
						Component.literal(String.valueOf(max)).withStyle(ChatFormatting.GOLD))
				.withStyle(ChatFormatting.AQUA));
		for (InfinityStoneType type : stones) {
			tooltip.add(Component.literal("  • ").append(
					Component.translatable("item.superheroes." + type.getItemRegistryName()))
					.withStyle(style -> style.withColor(type.getColor() & 0xFFFFFF)));
		}
		if (count == max) {
			tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.full").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		tooltip.add(Component.empty());
		tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.usage.transform").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.usage.insert").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.usage.eject").withStyle(ChatFormatting.RED));
		tooltip.add(Component.translatable("item.superheroes.infinity_gauntlet.usage.untransform").withStyle(ChatFormatting.RED));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return InfinityGauntletData.getStoneCount(stack) > 0;
	}

	@Override
	public void onCraftedBy(ItemStack stack, Level level, Player player) {
		ensureModelData(stack);
		super.onCraftedBy(stack, level, player);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
		ensureModelData(stack);
	}

	private static void ensureModelData(ItemStack stack) {
		int count = InfinityGauntletData.getStoneCount(stack);
		CustomModelData existing = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		if (existing == null || existing.value() != count) {
			stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(count));
		}
	}

	private static ItemStack stoneStack(InfinityStoneType type) {
		return switch (type) {
			case POWER -> new ItemStack(ModItems.POWER_STONE);
			case SPACE -> new ItemStack(ModItems.SPACE_STONE);
			case REALITY -> new ItemStack(ModItems.REALITY_STONE);
			case SOUL -> new ItemStack(ModItems.SOUL_STONE);
			case TIME -> new ItemStack(ModItems.TIME_STONE);
			case MIND -> new ItemStack(ModItems.MIND_STONE);
		};
	}
}
