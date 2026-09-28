package io.github.grebeshok105.codex.mcpdev;

import java.util.List;
import java.util.Map;

import com.chapmanjw.minecraft.fabric.mcp.compat.ToolCategory;
import com.chapmanjw.minecraft.fabric.mcp.protocol.Tool;
import com.chapmanjw.minecraft.fabric.mcp.tools.ToolProvider;

/**
 * The {@code mcp-tools} entrypoint of the dev-only companion mod: contributes the
 * codex_* tools to the upstream MCP bridge without the bridge ever importing
 * superheroes classes (the coupling direction is mcpdev -> both).
 *
 * <p>The {@code codex} tool-name domain is not in the upstream built-in category
 * map, and the compatibility filter hard-rejects unknown domains — so this
 * provider declares it explicitly via {@link #domainCategories()}, mapping to
 * {@link ToolCategory#SERVER} (enabled by default).
 */
public final class CodexMcpdevToolProvider implements ToolProvider {

	@Override
	public List<Class<? extends Tool>> toolClasses() {
		return List.of(
				CodexAbilityInvokeTool.class,
				CodexAbilityListTool.class,
				CodexCooldownsClearTool.class,
				CodexHeroListTool.class,
				CodexHeroSelectTool.class,
				CodexModStatusTool.class);
	}

	@Override
	public Map<String, ToolCategory> domainCategories() {
		return Map.of("codex", ToolCategory.SERVER);
	}
}
