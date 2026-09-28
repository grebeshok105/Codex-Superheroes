package io.github.grebeshok105.codex.mcpdev;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import com.chapmanjw.minecraft.fabric.mcp.protocol.error.ErrorCodes;
import com.chapmanjw.minecraft.fabric.mcp.protocol.error.McpException;

/**
 * Resolves user-supplied ids against a registry keyed by {@link ResourceLocation}.
 * Accepts {@code namespace:path} verbatim or a bare path, in which case the
 * {@code superheroes} namespace is tried first and any namespace whose path
 * matches case-insensitively after that — so {@code heat_vision} finds
 * {@code superheroes:heat_vision} without forcing callers to know the namespace.
 */
final class CodexMcpdevIds {

	private CodexMcpdevIds() {}

	static <T> ResourceLocation resolve(String raw, Map<ResourceLocation, T> registry, String kind) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			throw new McpException(ErrorCodes.TOOL_INPUT_INVALID, "Empty " + kind + " id");
		}
		if (trimmed.contains(":")) {
			ResourceLocation id = ResourceLocation.tryParse(trimmed);
			if (id != null && registry.containsKey(id)) {
				return id;
			}
		} else {
			ResourceLocation codexNs = ResourceLocation.fromNamespaceAndPath("superheroes", trimmed);
			if (registry.containsKey(codexNs)) {
				return codexNs;
			}
		}
		for (ResourceLocation key : registry.keySet()) {
			if (key.getPath().equalsIgnoreCase(trimmed)
					|| key.toString().equalsIgnoreCase(trimmed)) {
				return key;
			}
		}
		throw new McpException(ErrorCodes.TOOL_INPUT_INVALID,
				"Unknown " + kind + " id: " + trimmed + " (known: "
						+ String.join(", ", registry.keySet().stream().map(ResourceLocation::toString).sorted().toList())
						+ ")");
	}
}
