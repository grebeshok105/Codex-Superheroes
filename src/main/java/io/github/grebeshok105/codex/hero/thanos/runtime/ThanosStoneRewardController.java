package io.github.grebeshok105.codex.hero.thanos.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityGauntletData;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityGauntletItem;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStoneType;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStones;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class ThanosStoneRewardController {
	// Leaves never import the module root — the hero id stays a local literal.
	private static final ResourceLocation THANOS_ID = ModId.of("thanos");

	private ThanosStoneRewardController() {
	}

	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayer victim)) return;
			HeroData victimData = victim.getAttachedOrCreate(CoreAttachments.HERO_DATA);
			if (!victimData.hasHero()) return;
			InfinityStoneType stone = InfinityStones.rewardFor(victimData.heroId());
			if (stone == null) return;

			Entity killerEntity = source.getEntity();
			if (!(killerEntity instanceof ServerPlayer killer)) return;
			if (killer.getUUID().equals(victim.getUUID())) return;
			HeroData killerData = killer.getAttachedOrCreate(CoreAttachments.HERO_DATA);
			if (!killerData.hasHero() || !THANOS_ID.equals(killerData.heroId())) return;

			if (alreadyHasStone(killer, stone)) return;

			grantStone(killer, stone);
		});
	}

	private static boolean alreadyHasStone(ServerPlayer killer, InfinityStoneType stone) {
		for (int slot = 0; slot < killer.getInventory().getContainerSize(); slot++) {
			ItemStack stack = killer.getInventory().getItem(slot);
			if (stack.isEmpty()) continue;
			if (stack.is(InfinityStones.stoneItem(stone))) return true;
			if (stack.getItem() instanceof InfinityGauntletItem && InfinityGauntletData.hasStone(stack, stone)) return true;
		}
		return false;
	}

	private static void grantStone(ServerPlayer killer, InfinityStoneType stone) {
		ItemStack stack = new ItemStack(InfinityStones.stoneItem(stone));
		boolean inserted = killer.getInventory().add(stack);
		ServerLevel level = killer.serverLevel();
		if (!inserted) {
			ItemEntity ie = new ItemEntity(level, killer.getX(), killer.getY() + 1.0, killer.getZ(), stack);
			ie.setDeltaMovement(level.random.nextGaussian() * 0.05, 0.3, level.random.nextGaussian() * 0.05);
			level.addFreshEntity(ie);
		}
		level.playSound(null, killer.getX(), killer.getY(), killer.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, killer.getX(), killer.getY(), killer.getZ(),
				SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 1.6f);
		level.playSound(null, killer.getX(), killer.getY(), killer.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 0.7f);

		killer.displayClientMessage(
				Component.translatable("hero.superheroes.thanos.stone_reward",
						Component.translatable(stone.getStoneNameKey()).withStyle(ChatFormatting.LIGHT_PURPLE))
						.withStyle(ChatFormatting.GOLD), false);
	}
}
