package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.bootstrap.HeroModules;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.hero.scorpion.ScorpionAbilities;
import io.github.grebeshok105.codex.hero.scorpion.ScorpionHero;
import io.github.grebeshok105.codex.item.ModItemGroups;
import io.github.grebeshok105.codex.item.ModItems;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** A hero that compiles must also be complete: registered abilities and lang in both languages. */
public final class HeroCompletenessGameTests implements FabricGameTest {
	static JsonObject lang(String code) {
		String path = "/assets/superheroes/lang/" + code + ".json";
		try (InputStream in = HeroCompletenessGameTests.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyListedAbilityIsRegistered(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (Hero hero : Heroes.all().values()) {
			for (ResourceLocation id : hero.getAbilities()) {
				if (AbilityRegistry.get(id) == null) {
					problems.add(hero.getId() + " lists unregistered " + id);
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyHeroAndAbilityHasLangInBothLanguages(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (String code : List.of("en_us", "ru_ru")) {
			JsonObject json = lang(code);
			for (Hero hero : Heroes.all().values()) {
				ResourceLocation heroId = hero.getId();
				require(json, code, "hero." + heroId.getNamespace() + "." + heroId.getPath(), problems);
				for (ResourceLocation id : hero.getAbilities()) {
					String base = "ability." + id.getNamespace() + "." + id.getPath();
					require(json, code, base, problems);
					require(json, code, base + ".desc", problems);
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void superheroesTabIdIsStable(GameTestHelper helper) {
		helper.assertValueEqual(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(ModItemGroups.SUPERHEROES_TAB),
				ModId.of("superheroes"), "creative tab id");
		helper.succeed();
	}

	// D2a-1 — Scorpion is the first hero wired through a HeroModule: Heroes must hold the
	// module's own instance, and the module must have pushed all four abilities in.
	@GameTest(template = EMPTY_STRUCTURE)
	public void scorpionIsRegisteredThroughItsModule(GameTestHelper helper) {
		helper.assertTrue(HeroModules.ALL.stream().anyMatch(m -> Heroes.get(ScorpionHero.ID) == m.hero()),
				"Heroes registry must hold the ScorpionModule's hero instance");
		for (ResourceLocation id : List.of(ScorpionAbilities.SPEAR, ScorpionAbilities.HELLFIRE,
				ScorpionAbilities.FIRE_TELEPORT, ScorpionAbilities.HELL_BREATH)) {
			helper.assertTrue(AbilityRegistry.get(id) != null, "ability not registered: " + id);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void modulesCoverEveryHeroInRegistryOrder(GameTestHelper helper) {
		List<ResourceLocation> fromModules = HeroModules.ALL.stream().map(m -> m.hero().getId()).toList();
		List<ResourceLocation> registered = List.copyOf(Heroes.all().keySet());
		helper.assertTrue(fromModules.equals(registered), "modules " + fromModules + " vs registry " + registered);
		helper.assertTrue(registered.size() == 22, "22 heroes, got " + registered.size());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pandoraSuitIdIsStable(GameTestHelper helper) {
		// The registry id is persisted in player saves; it keeps the old name forever.
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(ModItems.PANDORA_SUIT);
		helper.assertTrue(ModId.of("doctor_strange_suit").equals(id),
				"Pandora suit registry id drifted: " + id);
		helper.succeed();
	}

	private static void require(JsonObject json, String code, String key, List<String> problems) {
		if (!json.has(key)) {
			problems.add(code + " missing " + key);
		}
	}
}
