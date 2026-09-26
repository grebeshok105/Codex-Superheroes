package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Golden check for hero-owned passive attribute sets (B2): applying and removing
 * each hero's {@code applyPassives}/{@code removePassives} must put exactly the
 * {@code superheroes}-namespaced modifiers recorded in
 * {@code golden/passive_modifiers.txt} on a freshly joined player — one
 * {@code heroId|attribute|id|amount|op} line per modifier, sorted. The modifier
 * ids are persisted in player NBT, so any drift in an id, value or operation is
 * caught byte-for-byte.
 */
public final class PassiveModifiersGameTests implements FabricGameTest {

	private static List<String> goldenLines() {
		String path = "/golden/passive_modifiers.txt";
		try (InputStream in = PassiveModifiersGameTests.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
					.filter(line -> !line.isEmpty())
					.toList();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void passiveAttributeIdsAreStable(GameTestHelper helper) {
		List<Holder.Reference<Attribute>> attributes = BuiltInRegistries.ATTRIBUTE.holders().toList();
		List<String> lines = new ArrayList<>();
		for (Hero hero : Heroes.all().values()) {
			ServerPlayer player = TestPlayers.join(helper);
			hero.applyPassives(player);
			for (Holder.Reference<Attribute> attribute : attributes) {
				AttributeInstance instance = player.getAttribute(attribute);
				if (instance == null) {
					continue;
				}
				for (AttributeModifier modifier : instance.getModifiers()) {
					if (ModId.MOD_ID.equals(modifier.id().getNamespace())) {
						lines.add(hero.getId().getPath() + "|" + attribute.key().location() + "|" + modifier.id()
								+ "|" + modifier.amount() + "|" + modifier.operation());
					}
				}
			}
			hero.removePassives(player);
			TestPlayers.leave(player);
		}
		List<String> actual = new ArrayList<>(lines);
		actual.sort(null);
		List<String> expected = goldenLines();
		helper.assertTrue(actual.equals(expected), diffMessage(expected, actual));
		helper.succeed();
	}

	private static String diffMessage(List<String> expected, List<String> actual) {
		int firstDiff = Math.min(expected.size(), actual.size());
		for (int i = 0; i < firstDiff; i++) {
			if (!expected.get(i).equals(actual.get(i))) {
				firstDiff = i;
				break;
			}
		}
		return "passive modifiers diverged from golden/passive_modifiers.txt: expected "
				+ expected.size() + " lines, got " + actual.size() + ", first diff at line " + (firstDiff + 1)
				+ "\n--- golden ---\n" + String.join("\n", expected)
				+ "\n--- actual ---\n" + String.join("\n", actual);
	}
}
