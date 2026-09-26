package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.transform.TransformationItem;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * B3 comparison phase: regenerates every transformation item's appendHoverText
 * output and compares it line-by-line against the golden file captured before
 * the migration ({@code /golden/transformation_lore.txt}). Any difference fails
 * the test with the full expected-vs-actual block.
 *
 * <p>Format: one line per item, {@code itemId|line1|line2|...} where each line is
 * {@code key@COLOR}, {@code EMPTY}, or {@code DIVIDER}. Lines with several
 * translatable parts (e.g. the stone hint) join their {@code key@COLOR} tokens
 * with {@code +}.
 */
public final class TransformationLoreGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void transformationItemLoreIsStable(GameTestHelper helper) throws IOException {
		List<String> actual = new ArrayList<>();
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
			actual.add(itemId + "|" + String.join("|", lines));
		}
		helper.assertFalse(actual.isEmpty(), "no transformation items found in the item registry");
		List<String> expected = goldenLines();
		if (!expected.equals(actual)) {
			int firstDiff = 0;
			while (firstDiff < Math.min(expected.size(), actual.size()) && expected.get(firstDiff).equals(actual.get(firstDiff))) {
				firstDiff++;
			}
			helper.assertTrue(false,
					"transformation item lore differs from golden/transformation_lore.txt (first differing line "
							+ (firstDiff + 1) + ", expected " + expected.size() + " lines, got " + actual.size() + ")"
							+ "\n=== expected ===\n" + String.join("\n", expected)
							+ "\n=== actual ===\n" + String.join("\n", actual));
		}
		helper.succeed();
	}

	private static List<String> goldenLines() throws IOException {
		String path = "/golden/transformation_lore.txt";
		try (InputStream in = TransformationLoreGameTests.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			List<String> lines = new ArrayList<>();
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
				if (!line.isEmpty()) {
					lines.add(line);
				}
			}
			return lines;
		}
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
