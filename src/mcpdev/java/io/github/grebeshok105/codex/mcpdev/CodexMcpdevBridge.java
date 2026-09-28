package io.github.grebeshok105.codex.mcpdev;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/**
 * Dev-only lifecycle bridge for the codex-mcpdev companion mod.
 *
 * <p>The upstream MCP adapter exposes no public MinecraftServer accessor, so this
 * initializer keeps the current server reference for the codex_* tools. Reads
 * happen on the server main thread via the tool's normal upstream dispatch;
 * this class never touches MCP protocol types.
 */
public final class CodexMcpdevBridge implements ModInitializer {

	private static volatile MinecraftServer server;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
	}

	/**
	 * The live server reference, or {@code null} before a world is loaded or after it stops.
	 * Volatile read — safe from any thread; callers on the server main thread use it directly.
	 */
	static MinecraftServer server() {
		return server;
	}

	/** Friendly version of the superheroes mod container, or "unknown" when absent. */
	static String modVersion() {
		return FabricLoader.getInstance()
				.getModContainer("superheroes")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
