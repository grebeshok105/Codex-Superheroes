package io.github.grebeshok105.codex.core.transform;

import net.minecraft.ChatFormatting;

import java.util.List;

/** Tooltip of a transformation item: a framed block of flavor lines, an empty line, then bullet lines. */
public record TransformationLore(ChatFormatting frame, List<Line> flavor, List<Line> bullets) {
	public record Line(String key, ChatFormatting color) {
	}

	public TransformationLore {
		flavor = List.copyOf(flavor);
		bullets = List.copyOf(bullets);
	}
}
