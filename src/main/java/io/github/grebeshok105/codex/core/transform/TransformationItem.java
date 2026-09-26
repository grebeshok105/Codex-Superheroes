package io.github.grebeshok105.codex.core.transform;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TransformationItem extends Item {
	private final ResourceLocation heroId;
	private final @Nullable TransformationLore lore;

	public TransformationItem(ResourceLocation heroId, Properties properties) {
		this(heroId, properties, null);
	}

	public TransformationItem(ResourceLocation heroId, Properties properties, @Nullable TransformationLore lore) {
		super(properties);
		this.heroId = heroId;
		this.lore = lore;
	}

	public ResourceLocation getHeroId() {
		return heroId;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (lore == null) {
			return;
		}
		TooltipFrame.openDivider(tooltip, lore.frame());
		for (TransformationLore.Line line : lore.flavor()) {
			tooltip.add(TooltipFrame.flavor(line.key(), line.color()));
		}
		tooltip.add(Component.empty());
		for (TransformationLore.Line line : lore.bullets()) {
			tooltip.add(TooltipFrame.bullet(line.key(), line.color()));
		}
		TooltipFrame.closeDivider(tooltip, lore.frame());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResultHolder.success(stack);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			boolean changed;
			if (player.isShiftKeyDown()) {
				changed = HeroTransformService.untransform(serverPlayer);
			} else {
				changed = HeroTransformService.transform(serverPlayer, heroId);
			}
			return changed ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
		}
		return InteractionResultHolder.pass(stack);
	}
}
