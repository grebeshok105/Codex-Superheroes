package com.example.superheroes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Project sanity gate for Codex Superheroes. Source- and resource-level checks for the invariants
 * that AGENTS.md declares: shared code stays server-safe, no Fabric internals, the two lang files
 * travel together, runtime audio is OGG-only, and the registration seams (heroes, controllers,
 * sounds, item models) are actually wired.
 *
 * <p>These are honest greps over source text and JSON, not bytecode analysis: a constant inlined by
 * javac or a class loaded by name can pass without leaving an edge. For structural bytecode rules,
 * add a dedicated tool later — do not quietly extend these patterns.
 */
public final class ProjectSanityTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();
	private static final Path MAIN_JAVA = ROOT.resolve("src/main/java");
	private static final Path CLIENT_JAVA = ROOT.resolve("src/client/java");
	private static final Path MAIN_RESOURCES = ROOT.resolve("src/main/resources");
	private static final Path GENERATED_RESOURCES = ROOT.resolve("src/main/generated");
	private static final List<Path> RESOURCE_ROOTS = List.of(MAIN_RESOURCES, GENERATED_RESOURCES);
	private static final Path SUPERHEROES_MOD = MAIN_JAVA.resolve("com/example/superheroes/SuperheroesMod.java");
	private static final Path FABRIC_MOD_JSON = MAIN_RESOURCES.resolve("fabric.mod.json");
	private static final String MOD_ID = "superheroes";

	private static final Pattern HERO_DATA_DIRECT_WRITE = Pattern.compile(
			"setAttached\\(\\s*(?:[\\w.]+\\.)?HERO_DATA\\b|removeAttached\\(\\s*(?:[\\w.]+\\.)?HERO_DATA\\b"
					+ "|ModNetworking\\.sync(?:HeroData|Resources)\\(");
	private static final Pattern FABRIC_IMPL_IMPORT = Pattern.compile("net\\.fabricmc\\.fabric\\.impl\\.");
	private static final Pattern CLIENT_ONLY_IMPORT = Pattern.compile("import\\s+net\\.minecraft\\.client\\.|import\\s+net\\.fabricmc\\.fabric\\.api\\.client\\.");
	private static final Pattern STATIC_INIT = Pattern.compile("public static void init\\(\\)");
	private static final Pattern SOUND_NAME = Pattern.compile("\"" + MOD_ID + ":([^\"]+)\"");
	private static final Pattern DIRECT_WORLD_MUTATION = Pattern.compile(
			"\\.(?:destroyBlock|removeBlock|setBlock|setBlockAndUpdate)\\(");

	private ProjectSanityTest() {}

	public static void main(String[] args) throws IOException {
		assertNoForbiddenFabricImplImports();
		assertMainStaysServerSafe();
		assertLangFilesInSync();
		assertSoundsJsonResolvesAndAudioIsOggOnly();
		assertItemModelsResolveToTextures();
		assertControllersAreWired();
		assertFabricModJsonSanity();
		assertHeroDataHasSingleWriter();
		assertWorldMutationsGoThroughPolicy();
		assertClientStatesRegisterReset();
		assertClientCooldownsUseLevelGameTime();
		assertNoHeroTypeDispatch();
		assertNoCyrillicLiterals();
		assertEntityLangNames();
		System.out.println("ProjectSanityTest passed");
	}

	// Hard rule: public Fabric APIs only — nothing under net.fabricmc.fabric.impl.* anywhere.
	private static void assertNoForbiddenFabricImplImports() throws IOException {
		for (Path root : List.of(MAIN_JAVA, CLIENT_JAVA)) {
			forEachJavaFile(root, file -> {
				String source = Files.readString(file);
				assert !FABRIC_IMPL_IMPORT.matcher(source).find()
						: file + " references net.fabricmc.fabric.impl.* internals — only net.fabricmc.fabric.api.* is allowed";
			});
		}
	}

	// HeroData has one writer (audit B2): read-modify-write through HeroDataStore, never a stale copy.
	private static void assertHeroDataHasSingleWriter() throws IOException {
		Path store = MAIN_JAVA.resolve("com/example/superheroes/transform/HeroDataStore.java");
		Path networking = MAIN_JAVA.resolve("com/example/superheroes/network/ModNetworking.java");
		forEachJavaFile(MAIN_JAVA, file -> {
			if (file.equals(store) || file.equals(networking)) {
				return;
			}
			String source = Files.readString(file);
			assert !HERO_DATA_DIRECT_WRITE.matcher(source).find()
					: file + " writes or syncs HERO_DATA directly — use HeroDataStore.update(player, fn)";
		});
	}

	// Audit B10: ability-driven block removal / terrain edits route through
	// WorldDestructionPolicy so protection mods (PlayerBlockBreakEvents, spawn
	// protection, mobGriefing) see every change. Raw level mutations live only
	// inside the policy itself.
	private static void assertWorldMutationsGoThroughPolicy() throws IOException {
		Path policy = MAIN_JAVA.resolve("com/example/superheroes/world/WorldDestructionPolicy.java");
		assert Files.exists(policy) : "WorldDestructionPolicy.java is missing";
		forEachJavaFile(MAIN_JAVA, file -> {
			if (file.equals(policy)) {
				return;
			}
			String source = Files.readString(file);
			assert !DIRECT_WORLD_MUTATION.matcher(source).find()
					: file + " mutates the world directly — route ability block breaks through WorldDestructionPolicy";
		});
	}

	// Debt 4: hero-specific branching lives in the Hero hooks (canUseAbility, getImpactStyle,
	// ...), not in `instanceof` checks scattered through generic code.
	private static void assertNoHeroTypeDispatch() throws IOException {
		Pattern heroDispatch = Pattern.compile("instanceof\\s+[\\w.]*\\w+Hero\\b");
		for (Path root : List.of(MAIN_JAVA, CLIENT_JAVA)) {
			forEachJavaFile(root, file -> {
				String source = Files.readString(file);
				assert !heroDispatch.matcher(source).find()
						: file + " dispatches on a Hero subtype — move the branch behind a Hero default hook";
			});
		}
	}

	// Hygiene (audit §3): player-facing strings live in the lang files, not hardcoded.
	private static void assertNoCyrillicLiterals() throws IOException {
		Pattern cyrillicLiteral = Pattern.compile("Component\\.literal\\(\"[^\"]*[\\u0400-\\u04FF]");
		for (Path root : List.of(MAIN_JAVA, CLIENT_JAVA)) {
			forEachJavaFile(root, file -> {
				String source = Files.readString(file);
				assert !cyrillicLiteral.matcher(source).find()
						: file + " hardcodes a cyrillic Component.literal — use a lang key via Component.translatable";
			});
		}
	}

	// Hard rule: src/main must load on a dedicated server — no client-only imports.
	private static void assertMainStaysServerSafe() throws IOException {
		forEachJavaFile(MAIN_JAVA, file -> {
			String source = Files.readString(file);
			Matcher matcher = CLIENT_ONLY_IMPORT.matcher(source);
			assert !matcher.find()
					: file + " imports client-only classes (" + matcher.group()
							+ ") — client code belongs in src/client; a dedicated server loads src/main";
		});
	}

	// Hard rule: en_us.json and ru_ru.json are updated together — identical key sets.
	private static void assertLangFilesInSync() throws IOException {
		Path lang = MAIN_RESOURCES.resolve("assets/" + MOD_ID + "/lang");
		JsonObject en = parseJsonObject(lang.resolve("en_us.json"));
		JsonObject ru = parseJsonObject(lang.resolve("ru_ru.json"));
		Set<String> enKeys = en.keySet();
		Set<String> ruKeys = ru.keySet();
		Set<String> onlyEn = new TreeSet<>(enKeys);
		onlyEn.removeAll(ruKeys);
		Set<String> onlyRu = new TreeSet<>(ruKeys);
		onlyRu.removeAll(enKeys);
		assert onlyEn.isEmpty() && onlyRu.isEmpty()
				: "lang key drift — en_us.json and ru_ru.json must carry the same keys.\n"
						+ "  only in en_us: " + onlyEn + "\n  only in ru_ru: " + onlyRu;
	}

	// Hard rule: runtime sounds are OGG Vorbis only, and every sounds.json entry resolves on disk.
	private static void assertSoundsJsonResolvesAndAudioIsOggOnly() throws IOException {
		Path soundsDir = MAIN_RESOURCES.resolve("assets/" + MOD_ID + "/sounds");
		if (Files.isDirectory(soundsDir)) {
			try (Stream<Path> files = Files.walk(soundsDir)) {
				for (Path file : files.filter(Files::isRegularFile).toList()) {
					assert file.toString().endsWith(".ogg")
							: "runtime audio must be OGG Vorbis (ffmpeg -i in.mp3 -c:a libvorbis -qscale:a 5 out.ogg): " + file;
				}
			}
		}
		JsonObject sounds = parseJsonObject(MAIN_RESOURCES.resolve("assets/" + MOD_ID + "/sounds.json"));
		int references = 0;
		for (Map.Entry<String, JsonElement> event : sounds.entrySet()) {
			JsonElement soundsList = event.getValue().getAsJsonObject().get("sounds");
			if (soundsList == null) {
				continue;
			}
			for (JsonElement element : soundsList.getAsJsonArray()) {
				String name = element.isJsonPrimitive()
						? element.getAsString()
						: element.getAsJsonObject().get("name").getAsString();
				if (!name.startsWith(MOD_ID + ":")) {
					continue;
				}
				references++;
				Path ogg = soundsDir.resolve(name.substring((MOD_ID + ":").length()) + ".ogg");
				assert Files.exists(ogg) : "sounds.json references missing file " + ogg + " (event " + event.getKey() + ")";
			}
		}
		assert references > 0 : "sounds.json has no " + MOD_ID + " sound references; this check would pass vacuously";
	}

	// Every item model's superheroes:item/* texture reference must resolve under textures/item/,
	// in either the hand-written or the generated resources root. Models with a minecraft:* parent
	// (e.g. template_spawn_egg) get their look from vanilla and need no mod textures.
	private static void assertItemModelsResolveToTextures() throws IOException {
		int models = 0;
		for (Path root : RESOURCE_ROOTS) {
			Path modelsDir = root.resolve("assets/" + MOD_ID + "/models/item");
			if (!Files.isDirectory(modelsDir)) {
				continue;
			}
			try (Stream<Path> files = Files.list(modelsDir)) {
				for (Path model : files.filter(path -> path.toString().endsWith(".json")).toList()) {
					models++;
					JsonObject json = JsonParser.parseString(Files.readString(model)).getAsJsonObject();
					boolean vanillaParent = json.has("parent")
							&& json.get("parent").getAsString().startsWith("minecraft:");
					boolean foundTexture = false;
					if (json.has("textures")) {
						for (Map.Entry<String, JsonElement> texture : json.getAsJsonObject("textures").entrySet()) {
							String ref = texture.getValue().getAsString();
							if (!ref.startsWith(MOD_ID + ":item/")) {
								continue;
							}
							foundTexture = true;
							String textureName = ref.substring((MOD_ID + ":item/").length()) + ".png";
							boolean exists = false;
							for (Path resourceRoot : RESOURCE_ROOTS) {
								if (Files.exists(resourceRoot.resolve("assets/" + MOD_ID + "/textures/item/" + textureName))) {
									exists = true;
									break;
								}
							}
							assert exists : "missing item texture " + textureName + " referenced by " + model;
						}
					}
					assert foundTexture || vanillaParent
							: "item model has no " + MOD_ID + ":item/* texture reference and no minecraft: parent: " + model;
				}
			}
		}
		assert models > 0 : "no item models found under either resources root; this check would pass vacuously";
	}

	// Every registered entity type needs a display name — unlocalized ids leak into subtitles,
	// death messages, and the debug overlay (audit "мелочи"). en_us is the source of truth;
	// ru_ru parity is covered by assertLangFilesInSync.
	private static void assertEntityLangNames() throws IOException {
		JsonObject en = parseJsonObject(MAIN_RESOURCES.resolve("assets/" + MOD_ID + "/lang/en_us.json"));
		Pattern entityId = Pattern.compile("(?:ModId\\.of|register)\\(\\s*\"([a-z_]+)\"");
		for (String file : List.of("com/example/superheroes/entity/ModEntities.java",
				"com/example/superheroes/horde/entity/HordeEntities.java")) {
			Matcher ids = entityId.matcher(Files.readString(MAIN_JAVA.resolve(file)));
			while (ids.find()) {
				assert en.has("entity." + MOD_ID + "." + ids.group(1))
						: "missing entity." + MOD_ID + "." + ids.group(1) + " in en_us.json (registered in " + file + ")";
			}
		}
	}

	// Convention: a *Controller that declares `public static void init()` must have that init() invoked
	// from SuperheroesMod.onInitialize() or from a hero/shared module (*Module.java). Removed in stage D2b,
	// when ticks and lifecycle move behind HeroModuleContext and no controller keeps a static init().
	private static void assertControllersAreWired() throws IOException {
		StringBuilder wiring = new StringBuilder(Files.readString(SUPERHEROES_MOD));
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path module : files.filter(p -> p.getFileName().toString().endsWith("Module.java")).toList()) {
				wiring.append('\n').append(Files.readString(module));
			}
		}
		String wiringSource = wiring.toString();
		int controllers = 0;
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Controller.java")).toList()) {
				if (!STATIC_INIT.matcher(Files.readString(file)).find()) {
					continue;
				}
				controllers++;
				String name = file.getFileName().toString().replace(".java", "");
				assert wiringSource.contains(name + ".init()")
						: file.getFileName() + " declares public static void init() but " + name
								+ ".init() is called neither from SuperheroesMod nor from a *Module";
			}
		}
		assert controllers > 0 : "no *Controller with static init() found; this check would pass vacuously";
	}

	// The shipped descriptor must parse, carry the superheroes id, and declare both entrypoints.
	private static void assertFabricModJsonSanity() throws IOException {
		JsonObject descriptor = parseJsonObject(FABRIC_MOD_JSON);
		assert MOD_ID.equals(descriptor.get("id").getAsString())
				: "fabric.mod.json id must be '" + MOD_ID + "'";
		JsonObject entrypoints = descriptor.getAsJsonObject("entrypoints");
		assert entrypoints != null : "fabric.mod.json has no entrypoints";
		assert entrypoints.has("main") && !entrypoints.getAsJsonArray("main").isEmpty()
				: "fabric.mod.json has no main entrypoint";
		assert entrypoints.has("client") && !entrypoints.getAsJsonArray("client").isEmpty()
				: "fabric.mod.json has no client entrypoint";
	}

	// Audit B15: every Client*State holder self-registers a reset with ClientSessionState
	// in its static block, so the disconnect path iterates ALL of them and no per-class
	// list in SuperheroesClient can go stale when a new state class appears. The few
	// session-state singletons outside that naming convention are pinned by name.
	private static void assertClientStatesRegisterReset() throws IOException {
		Pattern clientStateFile = Pattern.compile("Client[A-Za-z0-9]+State\\.java");
		List<String> namedSingletons = List.of(
				"ClientAbilityCooldowns.java",
				"JarvisDetectionHud.java", "MirrorWarpFlashHud.java", "RadialMenuHud.java");
		forEachJavaFile(CLIENT_JAVA, file -> {
			String name = file.getFileName().toString();
			boolean covered = clientStateFile.matcher(name).matches() && !name.equals("ClientSessionState.java");
			if (!covered && !namedSingletons.contains(name)) {
				return;
			}
			String source = Files.readString(file);
			assert source.contains("ClientSessionState.register(")
					: file + " holds session state but never calls ClientSessionState.register(...) — "
							+ "its state survives disconnects (audit B15)";
		});
	}

	// Audit B15: client cooldown deadlines come from the level's game time, never
	// LocalPlayer.tickCount — that resets on respawn and made the HUD show hours.
	private static void assertClientCooldownsUseLevelGameTime() throws IOException {
		Path cooldowns = CLIENT_JAVA.resolve("com/example/superheroes/client/ClientAbilityCooldowns.java");
		String source = Files.readString(cooldowns);
		assert !Pattern.compile("\\.tickCount\\b").matcher(source).find()
				: cooldowns + " must not derive cooldown deadlines from LocalPlayer.tickCount — "
						+ "it resets on respawn and broke the HUD (audit B15)";
	}

	private static JsonObject parseJsonObject(Path file) throws IOException {
		JsonElement element = JsonParser.parseString(Files.readString(file));
		assert element.isJsonObject() : "not a JSON object: " + file;
		return element.getAsJsonObject();
	}

	private static void forEachJavaFile(Path root, IOConsumer consumer) throws IOException {
		try (Stream<Path> files = Files.walk(root)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				consumer.accept(file);
			}
		}
	}

	@FunctionalInterface
	private interface IOConsumer {
		void accept(Path file) throws IOException;
	}
}
