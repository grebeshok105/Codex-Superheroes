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
	private static final Pattern HERO_FIELD = Pattern.compile("public static final \\w+Hero (\\w+) =", Pattern.MULTILINE);
	private static final Pattern HERO_REGISTER = Pattern.compile("register\\(\\s*(\\w+)\\s*\\)");
	private static final Pattern STATIC_INIT = Pattern.compile("public static void init\\(\\)");
	private static final Pattern SOUND_NAME = Pattern.compile("\"" + MOD_ID + ":([^\"]+)\"");

	private ProjectSanityTest() {}

	public static void main(String[] args) throws IOException {
		assertNoForbiddenFabricImplImports();
		assertMainStaysServerSafe();
		assertLangFilesInSync();
		assertSoundsJsonResolvesAndAudioIsOggOnly();
		assertItemModelsResolveToTextures();
		assertEveryHeroRegistered();
		assertControllersAreWired();
		assertFabricModJsonSanity();
		assertHeroDataHasSingleWriter();
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

	// Hero seam: every hero constant declared in Heroes.java must be registered.
	private static void assertEveryHeroRegistered() throws IOException {
		String heroes = Files.readString(MAIN_JAVA.resolve("com/example/superheroes/hero/Heroes.java"));
		Set<String> declared = new TreeSet<>();
		Matcher fields = HERO_FIELD.matcher(heroes);
		while (fields.find()) {
			declared.add(fields.group(1));
		}
		Set<String> registered = new TreeSet<>();
		Matcher registers = HERO_REGISTER.matcher(heroes);
		while (registers.find()) {
			registered.add(registers.group(1));
		}
		assert !declared.isEmpty() : "no hero constants found in Heroes.java; this check would pass vacuously";
		Set<String> missing = new TreeSet<>(declared);
		missing.removeAll(registered);
		assert missing.isEmpty()
				: "heroes declared but never registered in Heroes.java: " + missing
						+ " — every public static final XxxHero field needs a matching register(...) call";
	}

	// Convention: a *Controller that declares `public static void init()` must have that init()
	// invoked from SuperheroesMod.onInitialize(). Controllers wired elsewhere are renamed or moved,
	// never silently unregistered.
	private static void assertControllersAreWired() throws IOException {
		String modSource = Files.readString(SUPERHEROES_MOD);
		int controllers = 0;
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Controller.java")).toList()) {
				if (!STATIC_INIT.matcher(Files.readString(file)).find()) {
					continue;
				}
				controllers++;
				String name = file.getFileName().toString().replace(".java", "");
				assert modSource.contains(name + ".init()")
						: file.getFileName() + " declares public static void init() but "
								+ name + ".init() is never called in SuperheroesMod.onInitialize()";
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
