package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.CaptainAmericaHero;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.hero.LokiHero;
import com.example.superheroes.hero.NarutoHero;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.hero.SungJinwooHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.item.infinity.InfinityGauntletData;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ThanosStoneRewardController {
	private static final Map<ResourceLocation, InfinityStoneType> HERO_TO_STONE = new HashMap<>();

	static {
		HERO_TO_STONE.put(KratosHero.ID, InfinityStoneType.POWER);
		HERO_TO_STONE.put(NarutoHero.ID, InfinityStoneType.SPACE);
		HERO_TO_STONE.put(SungJinwooHero.ID, InfinityStoneType.REALITY);
		HERO_TO_STONE.put(LokiHero.ID, InfinityStoneType.MIND);
		HERO_TO_STONE.put(CaptainAmericaHero.ID, InfinityStoneType.SOUL);
		HERO_TO_STONE.put(RegulusHero.ID, InfinityStoneType.TIME);
	}

	private ThanosStoneRewardController() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayer victim)) return;
			HeroData victimData = victim.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!victimData.hasHero()) return;
			InfinityStoneType stone = HERO_TO_STONE.get(victimData.heroId());
			if (stone == null) return;

			Entity killerEntity = source.getEntity();
			if (!(killerEntity instanceof ServerPlayer killer)) return;
			if (killer.getUUID().equals(victim.getUUID())) return;
			HeroData killerData = killer.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!killerData.hasHero() || !ThanosHero.ID.equals(killerData.heroId())) return;

			if (alreadyHasStone(killer, stone)) return;

			grantStone(killer, stone);
		});
	}

	private static boolean alreadyHasStone(ServerPlayer killer, InfinityStoneType stone) {
		for (int slot = 0; slot < killer.getInventory().getContainerSize(); slot++) {
			ItemStack stack = killer.getInventory().getItem(slot);
			if (stack.isEmpty()) continue;
			if (stack.is(stoneItem(stone))) return true;
			if (stack.is(ModItems.INFINITY_GAUNTLET) && InfinityGauntletData.hasStone(stack, stone)) return true;
		}
		return false;
	}

	private static void grantStone(ServerPlayer killer, InfinityStoneType stone) {
		ItemStack stack = new ItemStack(stoneItem(stone));
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

	private static net.minecraft.world.item.Item stoneItem(InfinityStoneType stone) {
		return switch (stone) {
			case POWER -> ModItems.POWER_STONE;
			case SPACE -> ModItems.SPACE_STONE;
			case REALITY -> ModItems.REALITY_STONE;
			case SOUL -> ModItems.SOUL_STONE;
			case TIME -> ModItems.TIME_STONE;
			case MIND -> ModItems.MIND_STONE;
		};
	}
}
