package io.github.grebeshok105.codex.mcpdev;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.chapmanjw.minecraft.fabric.mcp.protocol.error.ErrorCodes;
import com.chapmanjw.minecraft.fabric.mcp.protocol.error.McpException;

/**
 * Shared player resolution for the codex_* tools. Dumb by design: parse, look up, throw.
 *
 * <p>Error codes survive only on the HTTP thread —
 * {@link com.chapmanjw.minecraft.fabric.mcp.tools.BaseTool#onMainThread} wraps every
 * exception thrown inside its main-thread work into {@code TOOL_HANDLER_ERROR} — so
 * {@link #parsePlayerArg(String)} and {@link #requireServer()} must run in
 * {@code execute()} before {@code onMainThread}, while
 * {@link #requireOnline(MinecraftServer, String, UUID)} runs inside it.
 */
final class CodexMcpdevPlayers {

	private CodexMcpdevPlayers() {}

	/**
	 * Result of parsing the {@code player} argument: exactly one field is non-null.
	 */
	record PlayerRef(String name, UUID uuid) {}

	/**
	 * Parses a {@code player} argument on the HTTP thread: a string containing a UUID
	 * resolves by uuid, anything else is treated as an exact player name. Whitespace
	 * and empty input are {@code TOOL_INPUT_INVALID}.
	 */
	static PlayerRef parsePlayerArg(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			throw new McpException(ErrorCodes.TOOL_INPUT_INVALID, "Empty player argument");
		}
		try {
			return new PlayerRef(null, UUID.fromString(trimmed));
		} catch (IllegalArgumentException notUuid) {
			return new PlayerRef(trimmed, null);
		}
	}

	/**
	 * Fail-closed server check on the HTTP thread (the bridge field is volatile), so
	 * {@code SERVER_NOT_RUNNING} actually reaches clients instead of being flattened
	 * inside the main-thread hop. Call in {@code execute()} before {@code onMainThread}.
	 */
	static MinecraftServer requireServer() {
		MinecraftServer server = CodexMcpdevBridge.server();
		if (server == null) {
			throw new McpException(ErrorCodes.SERVER_NOT_RUNNING, "Minecraft server is not running");
		}
		return server;
	}

	/**
	 * Resolves a parsed ref against the live server on the server main thread:
	 * an offline or unknown player is {@code TOOL_HANDLER_ERROR} (upstream
	 * "Player not online" idiom); a null server is guarded with the same code.
	 */
	static ServerPlayer requireOnline(MinecraftServer server, PlayerRef ref) {
		if (server == null) {
			throw new McpException(ErrorCodes.SERVER_NOT_RUNNING, "Minecraft server is not running");
		}
		ServerPlayer player = ref.uuid() != null
				? server.getPlayerList().getPlayer(ref.uuid())
				: server.getPlayerList().getPlayerByName(ref.name());
		if (player == null) {
			throw new McpException(ErrorCodes.TOOL_HANDLER_ERROR,
					"Player not online: " + (ref.uuid() != null ? ref.uuid() : ref.name()));
		}
		return player;
	}
}
