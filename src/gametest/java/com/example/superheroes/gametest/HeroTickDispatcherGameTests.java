package com.example.superheroes.gametest;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.hero.NarutoHero;
import com.example.superheroes.lifecycle.HeroTickDispatcher;
import com.example.superheroes.transform.HeroDataStore;
import com.example.superheroes.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Audit debt 3 — {@link HeroTickDispatcher} phases, the central dead-player skip,
 * and per-ability routing. Tasks registered here leak into later ticks of the same
 * JVM, so they are pure counters/appenders with no assertions inside.
 */
public class HeroTickDispatcherGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void playerTasksSkipDeadPlayers(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		Set<UUID> seen = new HashSet<>();
		HeroTickDispatcher.onPlayerTick((server, p, data) -> seen.add(p.getUUID()));
		var server = player.level().getServer();

		HeroTickDispatcher.tick(server);
		helper.assertTrue(seen.contains(player.getUUID()),
				"player task runs for a living player");

		seen.clear();
		player.kill();
		HeroTickDispatcher.tick(server);
		helper.assertTrue(!seen.contains(player.getUUID()),
				"dead players are skipped centrally (audit B17)");

		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void activeAbilityTasksRouteByIsActive(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		AtomicInteger calls = new AtomicInteger();
		HeroTickDispatcher.onActiveAbilityTick(AbilityIds.NARUTO_SAGE_MODE,
				(server, p, data) -> calls.incrementAndGet());
		var server = player.level().getServer();

		HeroTickDispatcher.tick(server);
		helper.assertTrue(calls.get() == 0, "no hero, no ability task");

		TestHeroes.transform(player, NarutoHero.ID);
		HeroTickDispatcher.tick(server);
		helper.assertTrue(calls.get() == 0, "ability task stays gated on isActive");

		HeroDataStore.update(player, d -> d.withActive(AbilityIds.NARUTO_SAGE_MODE, true));
		HeroTickDispatcher.tick(server);
		helper.assertTrue(calls.get() == 1, "ability task runs once for the active ability");

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void phasesRunInEnumOrder(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, NarutoHero.ID);
		HeroDataStore.update(player, d -> d.withActive(AbilityIds.NARUTO_SAGE_MODE, true));

		List<HeroTickDispatcher.Phase> order = new ArrayList<>();
		HeroTickDispatcher.onGlobalTick(server -> order.add(HeroTickDispatcher.Phase.GLOBAL));
		HeroTickDispatcher.onLevelTick((server, level) -> order.add(HeroTickDispatcher.Phase.LEVELS));
		HeroTickDispatcher.onPlayerTick((server, p, data) -> order.add(HeroTickDispatcher.Phase.PLAYERS));
		HeroTickDispatcher.onActiveAbilityTick(AbilityIds.NARUTO_SAGE_MODE,
				(server, p, data) -> order.add(HeroTickDispatcher.Phase.ABILITY_ACTIVE));

		HeroTickDispatcher.tick(player.level().getServer());
		int global = order.indexOf(HeroTickDispatcher.Phase.GLOBAL);
		int levels = order.indexOf(HeroTickDispatcher.Phase.LEVELS);
		int players = order.indexOf(HeroTickDispatcher.Phase.PLAYERS);
		int ability = order.indexOf(HeroTickDispatcher.Phase.ABILITY_ACTIVE);
		helper.assertTrue(global >= 0 && global < levels && levels < players && players < ability,
				"phases run GLOBAL→LEVELS→PLAYERS→ABILITY_ACTIVE, got " + order);

		helper.assertTrue(HeroTransformService.forceUntransform(player), "untransform");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
