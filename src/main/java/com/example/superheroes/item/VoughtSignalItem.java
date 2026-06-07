package com.example.superheroes.item;

import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class VoughtSignalItem extends Item {
	public VoughtSignalItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.RED);
		tooltip.add(TooltipFrame.flavor("item.superheroes.vought_signal.lore.line1", ChatFormatting.RED));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bulletWarn("item.superheroes.vought_signal.lore.usage", ChatFormatting.RED));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.RED);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		ServerLevel level = (ServerLevel) context.getLevel();
		BlockPos pos = context.getClickedPos().above();
		HomelanderBossEntity boss = ModEntities.HOMELANDER_BOSS.create(level);
		if (boss == null) {
			return InteractionResult.FAIL;
		}
		boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
				context.getHorizontalDirection().toYRot(), 0f);
		level.addFreshEntity(boss);

		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(boss.position());
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}

		level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.4f, 0.7f);

		if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.CONSUME;
	}
}
