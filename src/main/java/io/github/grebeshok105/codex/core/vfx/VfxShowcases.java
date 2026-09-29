package io.github.grebeshok105.codex.core.vfx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of named in-game showcase scenes driven by
 * {@code /superheroes vfx scene <id>} / {@code stress}. Hero-agnostic: hero
 * modules register their scenes at init; the command tree resolves and runs
 * them without knowing which hero owns what.
 */
public final class VfxShowcases {
	/** One named scene; {@code count} is the repetition count the caller asked for. */
	@FunctionalInterface
	public interface VfxShowcase {
		void run(ServerPlayer at, int count);
	}

	private static final Map<ResourceLocation, VfxShowcase> SCENES = new ConcurrentHashMap<>();

	private VfxShowcases() {
	}

	public static void register(ResourceLocation id, VfxShowcase scene) {
		SCENES.put(id, scene);
	}

	@Nullable
	public static VfxShowcase get(ResourceLocation id) {
		return SCENES.get(id);
	}

	public static Set<ResourceLocation> ids() {
		return Set.copyOf(SCENES.keySet());
	}
}
