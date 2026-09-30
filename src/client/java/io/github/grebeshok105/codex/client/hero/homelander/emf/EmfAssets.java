package io.github.grebeshok105.codex.client.hero.homelander.emf;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.anim.AnimationClip;
import io.github.grebeshok105.codex.client.core.anim.BedrockAnimationParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource-reload entry point for the Homelander EMF assets:
 * {@code assets/superheroes/emf/homelander/model.json} + clip JSONs, plus the
 * legacy {@code player_animations/homelander/*.animation.json} clips that were
 * kept (ported to {@link EmfClip}s by {@link LegacyClipPort} so they run on
 * the same expression runtime). Compiles the {@link HomelanderEmfEngine} and
 * installs it on {@link HomelanderEmfRuntime}. {@code reference_only} clips
 * (showcase.json, emf_lab_expressions.json) never enter the runtime.
 */
public record EmfAssets(HomelanderEmfEngine engine, List<EmfClip> clips,
		Set<String> clipNames) implements SimpleSynchronousResourceReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceLocation ID = ModId.of("emf/homelander");
	private static final String EMF_DIR = "emf/homelander";
	private static final String LEGACY_DIR = "player_animations/homelander";
	private static final String CLIP_SUFFIX = ".json";
	private static final String LEGACY_SUFFIX = ".animation.json";
	private static final String MODEL_FILE = "model.json";

	public static final EmfAssets EMPTY = new EmfAssets(null, List.of(), Set.of());

	public static void init() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
				.registerReloadListener(new EmfAssets(null, List.of(), Set.of()));
	}

	@Override
	public ResourceLocation getFabricId() {
		return ID;
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		EmfModelData modelData = null;
		List<EmfClip> clips = new ArrayList<>();
		for (Map.Entry<ResourceLocation, Resource> entry : manager
				.listResources(EMF_DIR, id -> id.getPath().endsWith(CLIP_SUFFIX)).entrySet()) {
			ResourceLocation file = entry.getKey();
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				if (file.getPath().endsWith("/" + MODEL_FILE)) {
					modelData = EmfModelData.parse(json);
					if (modelData == null) {
						LOGGER.warn("[emf] {} is not a codex-emf-model/1 file", file);
					}
				} else {
					// varName() derives from the id's path tail — strip ".json".
					String path = file.getPath();
					EmfClip clip = EmfClip.parse(file.withPath(
							path.substring(0, path.length() - CLIP_SUFFIX.length())), json);
					if (clip != null) {
						clips.add(clip);
					}
				}
			} catch (Exception e) {
				LOGGER.warn("[emf] failed to load {}", file, e);
			}
		}
		for (Map.Entry<ResourceLocation, Resource> entry : manager
				.listResources(LEGACY_DIR, id -> id.getPath().endsWith(LEGACY_SUFFIX)).entrySet()) {
			ResourceLocation file = entry.getKey();
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				for (AnimationClip legacy : BedrockAnimationParser.parse(
						JsonParser.parseReader(reader).getAsJsonObject(),
						w -> LOGGER.warn("[emf] {}: {}", file, w))) {
					clips.add(LegacyClipPort.port(legacy));
				}
			} catch (Exception e) {
				LOGGER.warn("[emf] failed to port legacy clip {}", file, e);
			}
		}
		if (modelData == null) {
			LOGGER.warn("[emf] no homelander model.json — EMF runtime disabled this reload");
			HomelanderEmfRuntime.install(EMPTY);
			return;
		}
		EmfExpressionBuilder.Built built = EmfExpressionBuilder.build(modelData, clips);
		HomelanderEmfEngine engine = HomelanderEmfEngine.compile(modelData, built.lines());
		Set<String> names = new LinkedHashSet<>();
		for (EmfClip clip : clips) {
			names.add(clip.varName());
		}
		HomelanderEmfRuntime.install(new EmfAssets(engine, clips, names));
		if (engine == null) {
			LOGGER.warn("[emf] engine compile failed — Homelander falls back to vanilla model");
		}
	}
}
