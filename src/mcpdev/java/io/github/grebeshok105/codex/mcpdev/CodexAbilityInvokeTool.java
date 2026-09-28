package io.github.grebeshok105.codex.mcpdev;

import com.fasterxml.jackson.databind.JsonNode;
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
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Invokes one ability for the target player through {@link AbilityRouter#activate} —
 * the same entry point input bindings use, so denial rules, hero gates, cooldowns,
 * resource costs, and toggle behaviour all apply exactly as in normal play.
 *
 * <p>{@link AbilityRouter#activate} is intentionally silent (failures surface
 * in-game as denial notifications), so the result reports the observable state
 * after the call — {@code active} flips for toggles, {@code cooldown_remaining_ticks}
 * rises on success — rather than claiming the cast was accepted.
 */
@McpTool(
		name = "codex_ability_invoke",
		description = "Invokes one ability for the target player through the normal router path.")
public final class CodexAbilityInvokeTool extends BaseTool {

	private static final JsonNode SCHEMA = Schemas.object()
			.required("player", Schemas.string("Player name or UUID"))
			.required("ability", Schemas.string("Ability id (e.g. superheroes:heat_vision)"))
			.build();

	public CodexAbilityInvokeTool() {
		super("codex_ability_invoke");
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
		ResourceLocation abilityId =
				CodexMcpdevIds.resolve(r.requireString("ability"), AbilityRegistry.all(), "ability");
		return onMainThread(
				context,
				ignored -> {
					ServerPlayer target = CodexMcpdevPlayers.requireOnline(server, player);
					HeroData before = HeroDataStore.get(target);
					boolean wasActive = before.isActive(abilityId);
					AbilityRouter.activate(target, abilityId);
					HeroData after = HeroDataStore.get(target);
					Ability ability = AbilityRegistry.get(abilityId);
					ObjectNode out = context.mapper().createObjectNode();
					out.put("player", target.getGameProfile().getName());
					out.put("ability", abilityId.toString());
					out.put("hero", after.hasHero() ? after.heroId().toString() : null);
					out.put("active", after.isActive(abilityId));
					out.put("cooldown_remaining_ticks",
							AbilityCooldowns.remainingTicks(target, abilityId));
					if (ability != null) {
						out.put("toggle", ability.isToggle());
						if (ability.isToggle()) {
							out.put("state_changed", wasActive != after.isActive(abilityId));
						}
					}
					return okToon(out);
				});
	}
}
