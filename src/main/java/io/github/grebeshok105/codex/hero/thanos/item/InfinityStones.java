package io.github.grebeshok105.codex.hero.thanos.item;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * Thanos-owned stone data: which hero drops which Infinity Stone (the six rows that
 * used to live scattered across {@code ThanosStoneRewardController}'s static table and
 * the other heroes' {@code registerHeroStone} calls). Hero ids stay literal strings —
 * the module never names another hero's classes.
 */
public final class InfinityStones {
	private static final Map<ResourceLocation, InfinityStoneType> REWARDS = Map.of(
			ModId.of("kratos"), InfinityStoneType.POWER,
			ModId.of("captain_america"), InfinityStoneType.SOUL,
			ModId.of("loki"), InfinityStoneType.MIND,
			ModId.of("naruto"), InfinityStoneType.SPACE,
			ModId.of("sung_jinwoo"), InfinityStoneType.REALITY,
			ModId.of("regulus"), InfinityStoneType.TIME
	);

	private InfinityStones() {
	}

	/** The stone a victim-hero drops to a Thanos killer, or {@code null} when unmapped. */
	public static InfinityStoneType rewardFor(ResourceLocation heroId) {
		return REWARDS.get(heroId);
	}

	/** Registry lookup — leaves never reach the module's root items class. */
	public static Item stoneItem(InfinityStoneType stone) {
		return BuiltInRegistries.ITEM.get(ModId.of(stone.getItemRegistryName()));
	}

	/** Registry lookup for the gauntlet itself (icon stacks, badge rendering). */
	public static Item gauntletItem() {
		return BuiltInRegistries.ITEM.get(ModId.of("infinity_gauntlet"));
	}
}
