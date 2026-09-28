package io.github.grebeshok105.codex.client.core.vfx.params;

import com.google.gson.JsonParser;
import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads every {@code assets/<ns>/vfx/<path>.json} into {@link VfxParams} keyed
 * by {@code <ns>:<path>}. Synchronous reload listener so {@code F3+T} re-tunes
 * live without a client restart.
 */
public final class VfxParamsLoader implements SimpleSynchronousResourceReloadListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-vfx");
	private static final ResourceLocation ID = ModId.of("vfx_params");
	private static final String DIRECTORY = "vfx";
	private static final String SUFFIX = ".json";
	private static final Map<ResourceLocation, VfxParams> PARAMS = new ConcurrentHashMap<>();

	public static VfxParams get(ResourceLocation id) {
		return PARAMS.getOrDefault(id, VfxParams.EMPTY);
	}

	@Override
	public ResourceLocation getFabricId() {
		return ID;
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Map<ResourceLocation, VfxParams> loaded = new HashMap<>();
		for (Map.Entry<ResourceLocation, Resource> entry
				: manager.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX)).entrySet()) {
			ResourceLocation file = entry.getKey();
			String path = file.getPath();
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
					path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length()));
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				loaded.put(id, VfxParams.parse(JsonParser.parseReader(reader).getAsJsonObject()));
			} catch (Exception e) {
				LOGGER.warn("failed to parse vfx params {}", file, e);
			}
		}
		PARAMS.clear();
		PARAMS.putAll(loaded);
	}
}
