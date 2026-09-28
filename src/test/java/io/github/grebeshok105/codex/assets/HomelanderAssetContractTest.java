package io.github.grebeshok105.codex.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Contract gate for the Visual Core + Homelander pilot
 * (docs/design/visual-core-homelander/shared-contract.md, enforced rows in
 * src/test/resources/contracts/homelander_pilot.json). Every sound event the runtime
 * references must be registered in sounds.json and backed by a real OGG Vorbis file of
 * roughly the declared duration; every clip must exist under player_animations/homelander/
 * with the declared length, loop mode and the six allowed player bones; models and
 * textures must exist at their contract paths. Disabled until Task 2 lands the
 * placeholder resources (mirrors finalBuildHasNoPlaceholders' lifecycle).
 */
@Disabled("enabled in Task 2 once placeholders exist")
class HomelanderAssetContractTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();
	private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/superheroes");
	private static final Path SOUNDS_DIR = ASSETS.resolve("sounds/homelander");
	private static final Path CLIPS_DIR = ASSETS.resolve("player_animations/homelander");
	private static final Path SOUNDS_JSON = ASSETS.resolve("sounds.json");
	private static final Path CONTRACT = ROOT.resolve("src/test/resources/contracts/homelander_pilot.json");
	private static final String CLIP_KEY_PREFIX = "animation.superheroes.homelander.";
	private static final String SOUND_NAME_PREFIX = "superheroes:homelander/";
	private static final Set<String> ALLOWED_BONES =
			Set.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");
	private static final String HOLD_ON_LAST_FRAME = "hold_on_last_frame";
	private static final double DURATION_TOLERANCE = 0.30;
	private static final long MIN_LOOP_SOUND_MS = 1000;
	private static final long MAX_CONTACT_MS = 120;
	private static final float EVENT_TIME_TOLERANCE_SECONDS = 0.05f;

	private record SoundRow(String event, String file, long durationMs, boolean loop) {}
	private record ClipRow(String clip, String file, long lengthMs, boolean loop, Map<String, Double> events) {}

	private static List<SoundRow> sounds;
	private static List<ClipRow> clips;
	private static List<String> models;
	private static List<String> textures;
	private static JsonObject soundsJson;

	@BeforeAll
	static void loadContract() throws IOException {
		JsonObject contract;
		try (Reader reader = Files.newBufferedReader(CONTRACT)) {
			contract = JsonParser.parseReader(reader).getAsJsonObject();
		}
		sounds = new ArrayList<>();
		for (JsonElement element : contract.getAsJsonArray("sounds")) {
			JsonObject row = element.getAsJsonObject();
			sounds.add(new SoundRow(
					row.get("event").getAsString(),
					row.get("file").getAsString(),
					row.get("durationMs").getAsLong(),
					row.get("loop").getAsBoolean()));
		}
		clips = new ArrayList<>();
		for (JsonElement element : contract.getAsJsonArray("animations")) {
			JsonObject row = element.getAsJsonObject();
			Map<String, Double> events = new HashMap<>();
			if (row.has("events")) {
				for (Map.Entry<String, JsonElement> entry : row.getAsJsonObject("events").entrySet()) {
					events.put(entry.getKey(), entry.getValue().getAsDouble());
				}
			}
			clips.add(new ClipRow(
					row.get("clip").getAsString(),
					row.get("file").getAsString(),
					row.get("lengthMs").getAsLong(),
					row.get("loop").getAsBoolean(),
					events));
		}
		models = new ArrayList<>();
		for (JsonElement element : contract.getAsJsonArray("models")) {
			models.add(element.getAsJsonObject().get("path").getAsString());
		}
		textures = new ArrayList<>();
		for (JsonElement element : contract.getAsJsonArray("textures")) {
			textures.add(element.getAsJsonObject().get("path").getAsString());
		}
		try (Reader reader = Files.newBufferedReader(SOUNDS_JSON)) {
			soundsJson = JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	@Test
	void everyContractSoundIsRegisteredAndOgg() throws IOException {
		for (SoundRow row : sounds) {
			String stem = row.file().replaceFirst("\\.ogg$", "");
			JsonElement eventEntry = soundsJson.get(row.event());
			assertNotNull(eventEntry, "sounds.json is missing event " + row.event());
			boolean referencesFile = false;
			for (JsonElement sound : eventEntry.getAsJsonObject().getAsJsonArray("sounds")) {
				JsonObject entry = sound.isJsonObject() ? sound.getAsJsonObject() : null;
				String name = entry != null ? entry.get("name").getAsString() : sound.getAsString();
				if (name.equals(SOUND_NAME_PREFIX + stem)) {
					referencesFile = true;
				}
			}
			assertTrue(referencesFile,
					"sounds.json event " + row.event() + " does not reference " + SOUND_NAME_PREFIX + stem);
			Path file = SOUNDS_DIR.resolve(row.file());
			assertTrue(Files.exists(file), "missing sound file " + file);
			try (InputStream in = Files.newInputStream(file)) {
				byte[] magic = in.readNBytes(4);
				assertEquals("OggS", new String(magic), file + " is not an OGG stream");
			}
		}
	}

	@Test
	void oneShotSoundDurationsWithinTolerance() throws IOException {
		for (SoundRow row : sounds) {
			Path file = SOUNDS_DIR.resolve(row.file());
			if (!Files.exists(file)) {
				continue; // missing resources are reported by everyContractSoundIsRegisteredAndOgg
			}
			long measured = OggInfo.durationMs(file);
			if (row.loop()) {
				assertTrue(measured >= MIN_LOOP_SOUND_MS,
						row.file() + " loop is only " + measured + " ms (< " + MIN_LOOP_SOUND_MS + ")");
			} else {
				long delta = Math.abs(measured - row.durationMs());
				assertTrue(delta <= row.durationMs() * DURATION_TOLERANCE,
						row.file() + " measured " + measured + " ms vs contract " + row.durationMs()
								+ " ms (±30 %)");
			}
		}
	}

	@Test
	void everyContractClipExistsWithLength() throws IOException {
		for (ClipRow row : clips) {
			JsonObject clip = parseClip(row);
			JsonObject animation = clip.getAsJsonObject("animations")
					.getAsJsonObject(CLIP_KEY_PREFIX + row.clip());
			assertNotNull(animation, row.file() + " lacks key " + CLIP_KEY_PREFIX + row.clip());
			assertLoopValue(animation, row);
			if (row.loop()) {
				JsonElement loop = animation.get("loop");
				assertTrue(loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean()
								&& loop.getAsBoolean(),
						row.clip() + " contract loop:true but clip does not loop");
			} else {
				JsonElement loop = animation.get("loop");
				boolean loops = loop != null && loop.isJsonPrimitive()
						&& loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
				assertFalse(loops, row.clip() + " contract loop:false but clip loops forever");
				long lengthMs = Math.round(animation.get("animation_length").getAsDouble() * 1000);
				long delta = Math.abs(lengthMs - row.lengthMs());
				assertTrue(delta <= row.lengthMs() * DURATION_TOLERANCE,
						row.clip() + " length " + lengthMs + " ms vs contract " + row.lengthMs()
								+ " ms (±30 %)");
			}
			JsonObject bones = animation.getAsJsonObject("bones");
			if (bones != null) {
				for (String bone : bones.keySet()) {
					assertTrue(ALLOWED_BONES.contains(bone),
							row.clip() + " animates disallowed bone " + bone);
				}
			}
		}
	}

	@Test
	void contactEventsAreEarly() {
		for (ClipRow row : clips) {
			Double contact = row.events().get("contact");
			if (contact != null) {
				assertTrue(contact <= MAX_CONTACT_MS,
						row.clip() + " contact event at " + contact + " ms (> " + MAX_CONTACT_MS + ")");
			}
		}
	}

	@Test
	@Disabled("enabled in Task 15")
	void contractClipEventTimesMatchManifest() throws IOException {
		for (ClipRow row : clips) {
			if (row.events().isEmpty()) {
				continue;
			}
			JsonObject clip = parseClip(row);
			JsonObject animation = clip.getAsJsonObject("animations")
					.getAsJsonObject(CLIP_KEY_PREFIX + row.clip());
			assertNotNull(animation, row.file() + " lacks key " + CLIP_KEY_PREFIX + row.clip());
			Map<String, Float> eventTimes = eventTimes(animation);
			for (Map.Entry<String, Double> declared : row.events().entrySet()) {
				Float actual = eventTimes.get(declared.getKey());
				assertNotNull(actual,
						row.clip() + " has no event " + declared.getKey() + " on its timeline");
				assertTrue(Math.abs(actual - declared.getValue() / 1000f) <= EVENT_TIME_TOLERANCE_SECONDS,
						row.clip() + " event " + declared.getKey() + " at " + actual
								+ " s vs contract " + declared.getValue() + " ms");
			}
		}
	}

	@Test
	void contractModelsAndTexturesExist() {
		for (String path : models) {
			assertTrue(Files.exists(ASSETS.resolve(path)), "missing model " + path);
		}
		for (String path : textures) {
			assertTrue(Files.exists(ASSETS.resolve(path)), "missing texture " + path);
		}
	}

	private static JsonObject parseClip(ClipRow row) throws IOException {
		Path file = CLIPS_DIR.resolve(row.file());
		assertTrue(Files.exists(file), "missing clip file " + file);
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private static void assertLoopValue(JsonObject animation, ClipRow row) {
		JsonElement loop = animation.get("loop");
		if (loop == null) {
			return;
		}
		boolean allowed = (loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean())
				|| (loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isString()
						&& loop.getAsString().equals(HOLD_ON_LAST_FRAME));
		assertTrue(allowed,
				row.clip() + " has unsupported loop value " + loop + " (true or hold_on_last_frame)");
	}

	/** Bedrock timeline events: sound_effects / particle_effects / events, name -> seconds. */
	private static Map<String, Float> eventTimes(JsonObject animation) {
		Map<String, Float> times = new HashMap<>();
		for (String key : List.of("sound_effects", "particle_effects", "events")) {
			JsonElement element = animation.get(key);
			if (element == null || !element.isJsonObject()) {
				continue;
			}
			for (Map.Entry<String, JsonElement> at : element.getAsJsonObject().entrySet()) {
				float seconds;
				try {
					seconds = Float.parseFloat(at.getKey());
				} catch (NumberFormatException e) {
					continue;
				}
				JsonElement payload = at.getValue();
				List<JsonElement> entries = new ArrayList<>();
				if (payload.isJsonArray()) {
					payload.getAsJsonArray().forEach(entries::add);
				} else {
					entries.add(payload);
				}
				for (JsonElement entry : entries) {
					if (!entry.isJsonObject()) {
						continue;
					}
					JsonObject obj = entry.getAsJsonObject();
					JsonElement name = obj.has("effect") ? obj.get("effect") : obj.get("name");
					if (name != null && name.isJsonPrimitive()) {
						times.put(name.getAsString(), seconds);
					}
				}
			}
		}
		return times;
	}
}
