package com.example.superheroes.gametest;

import com.example.superheroes.ModId;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dumps every {@code superheroes}-namespaced attribute modifier that each hero's
 * {@code applyPassives}/{@code removePassives} pair puts on a freshly joined
 * player — one {@code heroId|attribute|id|amount|op} line per modifier, sorted.
 * This is the golden source for {@code golden/passive_modifiers.txt} (B2): the
 * modifier ids are persisted in player NBT, so the dump pins them byte-for-byte
 * before {@code HeroAttributes} is dissolved into per-hero sets.
 */
public final class PassiveModifiersGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void passiveAttributeIdsAreStable(GameTestHelper helper) throws java.io.IOException {
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
		List<String> sorted = new ArrayList<>(lines);
		sorted.sort(null);
		Path out = Path.of("build/golden/passive_modifiers.txt");
		Files.createDirectories(out.getParent());
		Files.write(out, sorted);
		helper.succeed();
	}
}
