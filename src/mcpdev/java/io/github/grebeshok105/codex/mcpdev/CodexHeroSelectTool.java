package io.github.grebeshok105.codex.mcpdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.chapmanjw.minecraft.fabric.mcp.protocol.ArgumentReader;
import com.chapmanjw.minecraft.fabric.mcp.protocol.Schemas;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolContext;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolResult;
import com.chapmanjw.minecraft.fabric.mcp.tools.BaseTool;
import com.chapmanjw.minecraft.fabric.mcp.tools.annotations.McpTool;

import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Transforms the target player into a hero (or back with {@code hero: "none"}).
 * The transform itself goes through {@link HeroTransformService}, the same entry
 * point transformation items and the {@code /superheroes hero} command use, so
 * FX, attribute swaps, and session-state sweeps all apply.
 */
@McpTool(
		name = "codex_hero_select",
		description = "Transforms the target player into a hero, or back with hero \"none\".")
public final class CodexHeroSelectTool extends BaseTool {

	private static final JsonNode SCHEMA = Schemas.object()
			.required("player", Schemas.string("Player name or UUID"))
			.required("hero", Schemas.string("Hero id (e.g. superheroes:iron_man), or \"none\" to untransform"))
			.build();

	public CodexHeroSelectTool() {
		super("codex_hero_select");
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
		String heroArg = r.requireString("hero");
		boolean untransform = heroArg.equalsIgnoreCase("none")
				|| heroArg.equalsIgnoreCase("null")
				|| heroArg.isBlank();
		ResourceLocation heroId = untransform ? null : CodexMcpdevIds.resolve(heroArg, Heroes.all(), "hero");
		return onMainThread(
				context,
				ignored -> {
					ServerPlayer target = CodexMcpdevPlayers.requireOnline(server, player);
					boolean changed = untransform
							? HeroTransformService.untransform(target)
							: HeroTransformService.transform(target, heroId);
					HeroData data = HeroDataStore.get(target);
					ObjectNode out = context.mapper().createObjectNode();
					out.put("player", target.getGameProfile().getName());
					out.put("changed", changed);
					out.put("hero", data.hasHero() ? data.heroId().toString() : null);
					out.put("energy", data.energy());
					out.put("mana", data.mana());
					return okToon(out);
				});
	}
}
