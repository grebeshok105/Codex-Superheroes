package io.github.grebeshok105.codex.core.transform;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TooltipFrame {
	private static final String DIVIDER = "━━━━━━━━━━━━━━━━━━━━━━━━";

	private TooltipFrame() {
	}

	public static void openDivider(List<Component> tooltip, ChatFormatting color) {
		tooltip.add(Component.literal(DIVIDER).withStyle(color));
	}

	public static void closeDivider(List<Component> tooltip, ChatFormatting color) {
		tooltip.add(Component.literal(DIVIDER).withStyle(color));
	}

	public static void section(List<Component> tooltip, Component header) {
		tooltip.add(Component.empty());
		tooltip.add(header);
	}

	public static Component bullet(String translationKey, ChatFormatting color) {
		return Component.literal("▸ ").withStyle(color, ChatFormatting.BOLD)
				.append(Component.translatable(translationKey).withStyle(color));
	}

	public static Component bulletWarn(String translationKey, ChatFormatting color) {
		return Component.literal("⚠ ").withStyle(color, ChatFormatting.BOLD)
				.append(Component.translatable(translationKey).withStyle(color, ChatFormatting.ITALIC));
	}

	public static Component flavor(String translationKey, ChatFormatting color) {
		return Component.translatable(translationKey).withStyle(color, ChatFormatting.ITALIC);
	}
}
