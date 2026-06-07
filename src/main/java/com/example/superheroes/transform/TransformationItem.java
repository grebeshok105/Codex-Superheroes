package com.example.superheroes.transform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TransformationItem extends Item {
	private final ResourceLocation heroId;

	public TransformationItem(ResourceLocation heroId, Properties properties) {
		super(properties);
		this.heroId = heroId;
	}

	public ResourceLocation getHeroId() {
		return heroId;
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
