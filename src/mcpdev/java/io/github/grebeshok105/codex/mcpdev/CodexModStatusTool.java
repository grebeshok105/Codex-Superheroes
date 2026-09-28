package io.github.grebeshok105.codex.mcpdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.chapmanjw.minecraft.fabric.mcp.protocol.Schemas;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolContext;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolResult;
import com.chapmanjw.minecraft.fabric.mcp.tools.BaseTool;
import com.chapmanjw.minecraft.fabric.mcp.tools.annotations.McpTool;

import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * One-shot health/status snapshot of the Codex runtime: mod version, registry
 * sizes, and every online player with their current hero and resource levels.
 */
@McpTool(
		name = "codex_mod_status",
		description = "Reports the superheroes mod version, registry sizes, and online players' hero state.")
public final class CodexModStatusTool extends BaseTool {

	private static final JsonNode SCHEMA = Schemas.object().build();

	public CodexModStatusTool() {
		super("codex_mod_status");
	}

	@Override
	public JsonNode inputSchema() {
		return SCHEMA;
	}

	@Override
	public ToolResult execute(JsonNode arguments, ToolContext context) {
		MinecraftServer server = CodexMcpdevPlayers.requireServer();
		return onMainThread(
				context,
				ignored -> {
					ObjectNode out = context.mapper().createObjectNode();
					out.put("mod_version", CodexMcpdevBridge.modVersion());
					out.put("heroes_registered", Heroes.all().size());
					out.put("abilities_registered", AbilityRegistry.all().size());
					ArrayNode players = out.putArray("players");
					for (ServerPlayer player : server.getPlayerList().getPlayers()) {
						ObjectNode node = players.addObject();
						node.put("name", player.getGameProfile().getName());
						node.put("uuid", player.getUUID().toString());
						HeroData data = HeroDataStore.get(player);
						node.put("hero", data.hasHero() ? data.heroId().toString() : null);
						node.put("energy", data.energy());
						node.put("mana", data.mana());
					}
					out.put("players_online", players.size());
					return okToon(out);
				});
	}
}
