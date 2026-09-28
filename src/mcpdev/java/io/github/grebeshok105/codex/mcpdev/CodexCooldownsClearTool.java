package io.github.grebeshok105.codex.mcpdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.chapmanjw.minecraft.fabric.mcp.protocol.ArgumentReader;
import com.chapmanjw.minecraft.fabric.mcp.protocol.Schemas;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolContext;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolResult;
import com.chapmanjw.minecraft.fabric.mcp.tools.BaseTool;
import com.chapmanjw.minecraft.fabric.mcp.tools.annotations.McpTool;

import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Drops every ability cooldown for the target player and syncs the client HUD,
 * via the same {@link AbilityCooldowns#clearAndSync} sweep the game uses.
 */
@McpTool(
		name = "codex_cooldowns_clear",
		description = "Clears all ability cooldowns for the target player and syncs the client.")
public final class CodexCooldownsClearTool extends BaseTool {

	private static final JsonNode SCHEMA = Schemas.object()
			.required("player", Schemas.string("Player name or UUID"))
			.build();

	public CodexCooldownsClearTool() {
		super("codex_cooldowns_clear");
	}

	@Override
	public JsonNode inputSchema() {
		return SCHEMA;
	}

	@Override
	public ToolResult execute(JsonNode arguments, ToolContext context) {
		ArgumentReader r = reader(arguments);
		MinecraftServer server = CodexMcpdevPlayers.requireServer();
		CodexMcpdevPlayers.PlayerRef player = CodexMcpdevPlayers.parsePlayerArg(r.requireString("player"));
		return onMainThread(
				context,
				ignored -> {
					ServerPlayer target = CodexMcpdevPlayers.requireOnline(server, player);
					AbilityCooldowns.clearAndSync(target);
					ObjectNode out = context.mapper().createObjectNode();
					out.put("player", target.getGameProfile().getName());
					out.put("cleared", true);
					return okToon(out);
				});
	}
}
