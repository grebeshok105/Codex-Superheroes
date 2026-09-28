package io.github.grebeshok105.codex.client.core.anim;

import com.google.gson.JsonParser;
import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loaded {@link AnimationClip}s keyed by their parsed id ({@code <ns>:<hero>/<clip>}).
 * Reloads synchronously from {@code assets/<ns>/player_animations/<path>.animation.json}
 * on every namespace, so {@code F3+T} picks up changed clips live; clips that fail to
 * parse are logged and skipped, never thrown.
 */
public final class AnimationLibrary implements SimpleSynchronousResourceReloadListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-anim");
	private static final ResourceLocation ID = ModId.of("player_animations");
	private static final String DIRECTORY = "player_animations";
	private static final String SUFFIX = ".animation.json";
	private static final Map<ResourceLocation, AnimationClip> CLIPS = new ConcurrentHashMap<>();

	public static Optional<AnimationClip> get(ResourceLocation id) {
		return Optional.ofNullable(CLIPS.get(id));
	}

	/** Registers the reload listener; called once from the client bootstrap. */
	public static void init() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
				.registerReloadListener(new AnimationLibrary());
	}

	@Override
	public ResourceLocation getFabricId() {
		return ID;
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Map<ResourceLocation, AnimationClip> loaded = new HashMap<>();
		for (Map.Entry<ResourceLocation, Resource> entry : manager
				.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX)).entrySet()) {
			ResourceLocation file = entry.getKey();
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				List<AnimationClip> clips = BedrockAnimationParser.parse(
						JsonParser.parseReader(reader).getAsJsonObject(),
						w -> LOGGER.warn("{}: {}", file, w));
				for (AnimationClip clip : clips) {
					loaded.put(clip.id(), clip);
				}
			} catch (Exception e) {
				LOGGER.warn("failed to parse animation {}", file, e);
			}
		}
		CLIPS.clear();
		CLIPS.putAll(loaded);
	}
}
