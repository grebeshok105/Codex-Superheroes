package io.github.grebeshok105.codex.mcpdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.chapmanjw.minecraft.fabric.mcp.protocol.Schemas;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolContext;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolResult;
import com.chapmanjw.minecraft.fabric.mcp.tools.BaseTool;
import com.chapmanjw.minecraft.fabric.mcp.tools.annotations.McpTool;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;

import net.minecraft.resources.ResourceLocation;

/**
 * Lists every registered hero with its ability ids.
 */
@McpTool(
		name = "codex_hero_list",
		description = "Lists every registered hero and its ability ids.")
public final class CodexHeroListTool extends BaseTool {

	private static final JsonNode SCHEMA = Schemas.object().build();

	public CodexHeroListTool() {
		super("codex_hero_list");
	}

	@Override
	public JsonNode inputSchema() {
		return SCHEMA;
	}

	@Override
	public ToolResult execute(JsonNode arguments, ToolContext context) {
		CodexMcpdevPlayers.requireServer();
		return onMainThread(
				context,
				ignored -> {
					ArrayNode heroes = context.mapper().createArrayNode();
					for (Hero hero : Heroes.all().values()) {
						ObjectNode node = context.mapper().createObjectNode();
						node.put("id", hero.getId().toString());
						ArrayNode abilities = node.putArray("abilities");
						for (ResourceLocation abilityId : hero.getAbilities()) {
							abilities.add(abilityId.toString());
						}
						heroes.add(node);
					}
					ObjectNode out = context.mapper().createObjectNode();
					out.put("count", heroes.size());
					out.set("heroes", heroes);
					return okToon(out);
				});
	}
}
