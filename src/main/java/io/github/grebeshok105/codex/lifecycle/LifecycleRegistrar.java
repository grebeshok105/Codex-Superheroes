package io.github.grebeshok105.codex.lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/** Where modules subscribe to player, hero and server lifecycle; backed by {@link PlayerLifecycle} (BF3) and {@link HeroLifecycle} (BF11). */
public interface LifecycleRegistrar {
	void onJoin(Consumer<ServerPlayer> hook);

	void onLeave(Consumer<ServerPlayer> hook);

	void onDeath(Consumer<ServerPlayer> hook);

	void onRespawn(Consumer<ServerPlayer> hook);

	void onServerStopped(Consumer<MinecraftServer> hook);

	/** Hero swap or untransform: {@link HeroLifecycle#onClear}. */
	void onHeroClear(Consumer<ServerPlayer> hook);

	void onHeroTransformed(java.util.function.BiConsumer<ServerPlayer, net.minecraft.resources.ResourceLocation> hook);

	static LifecycleRegistrar global() {
		return GlobalLifecycleRegistrar.INSTANCE;
	}
}
