package io.github.grebeshok105.codex.mcpdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.chapmanjw.minecraft.fabric.mcp.protocol.ArgumentReader;
import com.chapmanjw.minecraft.fabric.mcp.protocol.Schemas;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolContext;
import com.chapmanjw.minecraft.fabric.mcp.protocol.ToolResult;
import com.chapmanjw.minecraft.fabric.mcp.tools.BaseTool;
import com.chapmanjw.minecraft.fabric.mcp.tools.annotations.McpTool;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.model.ResourceKind;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player ability sheet: the current hero's abilities with binding, active
 * flag, remaining cooldown, toggle flag, and costs.
 */
@McpTool(
		name = "codex_ability_list",
		description = "Lists the target player's hero abilities with cooldown, active state, and costs.")
public final class CodexAbilityListTool extends BaseTool {

	private static final JsonNode SCHEMA = Schemas.object()
			.required("player", Schemas.string("Player name or UUID"))
			.build();

	public CodexAbilityListTool() {
		super("codex_ability_list");
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
					HeroData data = HeroDataStore.get(target);
					ObjectNode out = context.mapper().createObjectNode();
					out.put("player", target.getGameProfile().getName());
					out.put("hero", data.hasHero() ? data.heroId().toString() : null);
					out.put("energy", data.energy());
					out.put("mana", data.mana());
					ArrayNode abilities = out.putArray("abilities");
					if (data.hasHero()) {
						Hero hero = Heroes.get(data.heroId());
						if (hero != null) {
							for (ResourceLocation abilityId : hero.getAbilities()) {
								ObjectNode node = abilities.addObject();
								node.put("id", abilityId.toString());
								ResourceKind binding =
										data.binding(abilityId, hero.getDefaultBinding(abilityId));
								if (binding != null) {
									node.put("binding", binding.getSerializedName());
								}
								node.put("active", data.isActive(abilityId));
								node.put("cooldown_remaining_ticks",
										AbilityCooldowns.remainingTicks(target, abilityId));
								Ability ability = AbilityRegistry.get(abilityId);
								if (ability != null) {
									node.put("toggle", ability.isToggle());
									node.put("cost_on_activate", ability.costOnActivate());
									node.put("cost_per_tick", ability.costPerTick());
								}
							}
						}
					}
					return okToon(out);
				});
	}
}
