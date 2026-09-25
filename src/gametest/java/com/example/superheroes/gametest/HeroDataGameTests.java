package com.example.superheroes.gametest;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.hero.ScaramoucheHero;
import com.example.superheroes.transform.HeroData;
import com.example.superheroes.transform.HeroDataStore;
import com.example.superheroes.transform.HeroTransformService;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Audit B2: state written during an ability tick must not be overwritten by a stale copy. */
public final class HeroDataGameTests implements FabricGameTest {
	private static final int WIND_PRISON_DURATION_TICKS = 8 * 20;

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = WIND_PRISON_DURATION_TICKS + 80)
	public void windPrisonEndsWhenItsZoneExpires(GameTestHelper helper) {
		ServerPlayer scaramouche = TestPlayers.join(helper);
		TestHeroes.transform(scaramouche, ScaramoucheHero.ID);
		AbilityRouter.activate(scaramouche, AbilityIds.SCARAMOUCHE_WIND_PRISON);
		helper.assertTrue(HeroDataStore.get(scaramouche).isActive(AbilityIds.SCARAMOUCHE_WIND_PRISON),
				"Wind Prison starts");

		// Before the fix ResourceController wrote its tick-start copy back after onTickActive had
		// deactivated the expired zone, so the ability stayed "active" forever (cost 1.2 < regen 1.6).
		helper.runAfterDelay(WIND_PRISON_DURATION_TICKS + 20, () -> {
			helper.assertFalse(HeroDataStore.get(scaramouche).isActive(AbilityIds.SCARAMOUCHE_WIND_PRISON),
					"Wind Prison ends with its zone");
			TestPlayers.leave(scaramouche);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void deactivationIsNotUndoneByTheSameTick(GameTestHelper helper) {
		ServerPlayer scaramouche = TestPlayers.join(helper);
		HeroTransformService.transform(scaramouche, ScaramoucheHero.ID);
		AbilityRouter.activate(scaramouche, AbilityIds.SCARAMOUCHE_WIND_PRISON);
		AbilityRouter.deactivate(scaramouche, AbilityIds.SCARAMOUCHE_WIND_PRISON);

		helper.runAfterDelay(5, () -> {
			helper.assertFalse(HeroDataStore.get(scaramouche).isActive(AbilityIds.SCARAMOUCHE_WIND_PRISON),
					"a deactivated toggle stays off across ticks");
			TestPlayers.leave(scaramouche);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void decodesUnknownAbilityIds(GameTestHelper helper) {
		// Saves from before an ability was removed still hold its id in `active`; the codec
		// must keep decoding them as plain data (ResourceLocation.CODEC does not consult the registry).
		CompoundTag nbt = new CompoundTag();
		ListTag active = new ListTag();
		active.add(StringTag.valueOf("superheroes:meteor_slam"));
		nbt.put("active", active);

		DataResult<HeroData> parsed = HeroData.CODEC.parse(NbtOps.INSTANCE, nbt);
		helper.assertTrue(parsed.result().isPresent(),
				"HeroData.CODEC decodes nbt whose active list holds an unregistered ability id");
		helper.assertTrue(parsed.result().get().isActive(ModId.of("meteor_slam")),
				"the unregistered id survives as data");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void activatingAnUnlistedAbilityIdIsANoOp(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ScaramoucheHero.ID);
		ResourceLocation removed = ModId.of("meteor_slam");

		float energyBefore = HeroDataStore.get(player).energy();
		AbilityRouter.activate(player, removed);

		helper.assertFalse(HeroDataStore.get(player).isActive(removed),
				"an ability the hero does not list stays inactive");
		helper.assertTrue(HeroDataStore.get(player).energy() >= energyBefore,
				"no resource was charged for the no-op activation");
		TestPlayers.leave(player);
		helper.succeed();
	}
}
