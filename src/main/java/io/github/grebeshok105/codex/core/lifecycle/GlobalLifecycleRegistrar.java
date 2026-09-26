package io.github.grebeshok105.codex.core.lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

enum GlobalLifecycleRegistrar implements LifecycleRegistrar {
	INSTANCE;

	@Override public void onJoin(Consumer<ServerPlayer> hook) { PlayerLifecycle.onJoin(hook); }
	@Override public void onLeave(Consumer<ServerPlayer> hook) { PlayerLifecycle.onLeave(hook); }
	@Override public void onDeath(Consumer<ServerPlayer> hook) { PlayerLifecycle.onDeath(hook); }
	@Override public void onRespawn(Consumer<ServerPlayer> hook) { PlayerLifecycle.onRespawn(hook); }
	@Override public void onServerStopped(Consumer<MinecraftServer> hook) { PlayerLifecycle.onServerStopped(hook); }
	@Override public void onHeroClear(Consumer<ServerPlayer> hook) { HeroLifecycle.onClear(hook); }
	@Override public void onHeroTransformed(java.util.function.BiConsumer<ServerPlayer, net.minecraft.resources.ResourceLocation> hook) { HeroLifecycle.onTransformed(hook); }
}
