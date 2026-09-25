package com.example.superheroes.gametest;

import com.example.superheroes.transform.HeroTransformService;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Transforms through the same service the transformation item uses. */
final class TestHeroes {
	private TestHeroes() {
	}

	static void transform(ServerPlayer player, ResourceLocation heroId) {
		if (!HeroTransformService.transform(player, heroId)) {
			throw new GameTestAssertException("could not transform " + player.getScoreboardName() + " into " + heroId);
		}
	}
}
