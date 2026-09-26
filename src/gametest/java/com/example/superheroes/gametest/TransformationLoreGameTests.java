package com.example.superheroes.gametest;

import com.example.superheroes.transform.TransformationItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * B3 snapshot phase: dumps every transformation item's appendHoverText output to
 * {@code build/golden/transformation_lore.txt} (relative to the gametest working
 * directory, so it lands under {@code build/gametest/}). The orchestrator copies
 * this file and diffs it against the post-migration dump — it is a snapshot
 * writer, not a comparison golden.
 *
 * <p>Format: one line per item, {@code itemId|line1|line2|...} where each line is
 * {@code key@COLOR}, {@code EMPTY}, or {@code DIVIDER}. Lines with several
 * translatable parts (e.g. the stone hint) join their {@code key@COLOR} tokens
 * with {@code +}.
 */
public final class TransformationLoreGameTests implements FabricGameTest {
	private static final Path GOLDEN = Path.of("build/golden", "transformation_lore.txt");

	@GameTest(template = EMPTY_STRUCTURE)
	public void transformationItemLoreIsStable(GameTestHelper helper) throws IOException {
		List<String> out = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (!(item instanceof TransformationItem)) {
				continue;
			}
			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
			List<Component> tooltip = new ArrayList<>();
			item.appendHoverText(new ItemStack(item), Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
			List<String> lines = new ArrayList<>();
			for (Component line : tooltip) {
				lines.add(serializeLine(line));
			}
			out.add(itemId + "|" + String.join("|", lines));
		}
		helper.assertFalse(out.isEmpty(), "no transformation items found in the item registry");
		Files.createDirectories(GOLDEN.getParent());
		Files.write(GOLDEN, out, StandardCharsets.UTF_8);
		helper.succeed();
	}

	private static String serializeLine(Component line) {
		if (line.getContents() instanceof PlainTextContents plain && line.getSiblings().isEmpty()) {
			String text = plain.text();
			if (text.isEmpty()) {
				return "EMPTY";
			}
			if (text.chars().allMatch(c -> c == '━')) {
				return "DIVIDER";
			}
		}
		List<String> parts = new ArrayList<>();
		collectTranslatables(line, parts);
		if (parts.isEmpty()) {
			return line.getString();
		}
		return String.join("+", parts);
	}

	private static void collectTranslatables(Component component, List<String> out) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			out.add(translatable.getKey() + "@" + colorName(component.getStyle().getColor()));
		}
		for (Component sibling : component.getSiblings()) {
			collectTranslatables(sibling, out);
		}
	}

	private static String colorName(TextColor color) {
		if (color == null) {
			return "NONE";
		}
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (formatting.isColor() && Objects.equals(formatting.getColor(), color.getValue())) {
				return formatting.getName().toUpperCase(Locale.ROOT);
			}
		}
		return color.serialize();
	}
}
