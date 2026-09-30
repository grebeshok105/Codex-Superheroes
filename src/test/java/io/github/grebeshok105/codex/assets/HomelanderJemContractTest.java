package io.github.grebeshok105.codex.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract gate for the EMF-baked Homelander player model (plan §7 stage 1):
 * the generated {@code player.jem} / {@code player_slim.jem} shipped under
 * {@code assets/minecraft/emf/cem/} must carry every required part, every
 * custom id from the bbmodel, all five clips, boolean-valued visible
 * channels, and nothing forbidden (no {@code emf_lab}, no {@code cloak},
 * no NaN literals).
 */
class HomelanderJemContractTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();
	private static final Path CEM = ROOT.resolve("src/main/resources/assets/minecraft/emf/cem");
	private static final Path BBMODEL =
			ROOT.resolve("art-source/homelander/emf/Homelander_All_Animations.bbmodel");
	private static final Set<String> REQUIRED_CARRIERS = Set.of(
			"head", "body", "right_arm", "left_arm", "right_leg", "left_leg");
	private static final Set<String> HIDDEN_PARTS = Set.of(
			"headwear", "jacket", "left_sleeve", "right_sleeve", "left_pants", "right_pants");
	private static final Set<String> REQUIRED_IDS = Set.of(
			"hl_head", "hl_body", "hl_right_arm", "hl_left_arm", "hl_right_leg", "hl_left_leg",
			"hl_mouth_open", "hl_right_forearm", "hl_left_forearm",
			"hl_right_shin", "hl_right_knee", "hl_right_foot",
			"hl_left_shin", "hl_left_knee", "hl_left_foot",
			"hl_milk_bottle", "hl_milk_cap");
	private static final Set<String> REQUIRED_CLIPS = Set.of(
			"hover", "takeoff", "boost", "clap", "milk");
	private static final Set<String> REQUIRED_VARS;
	static {
		Set<String> vars = new HashSet<>(Set.of("superheroes_hl_w"));
		for (String clip : REQUIRED_CLIPS) {
			vars.add("superheroes_hl_" + clip + "_t");
		}
		for (String w : List.of("takeoff", "clap", "milk", "boost")) {
			vars.add("superheroes_hl_" + w + "_w");
		}
		REQUIRED_VARS = Set.copyOf(vars);
	}
	private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

	private static JsonObject jem;
	private static JsonObject jemSlim;
	private static String jemText;
	private static String jemSlimText;

	@BeforeAll
	static void loadJems() throws IOException {
		jem = readJson(CEM.resolve("player.jem"));
		jemSlim = readJson(CEM.resolve("player_slim.jem"));
		jemText = Files.readString(CEM.resolve("player.jem"));
		jemSlimText = Files.readString(CEM.resolve("player_slim.jem"));
	}

	@Test
	void bothJemsExist() {
		assertTrue(Files.exists(CEM.resolve("player.jem")), "player.jem missing");
		assertTrue(Files.exists(CEM.resolve("player_slim.jem")), "player_slim.jem missing");
	}

	@Test
	void requiredPartsAndCustomIdsPresent() {
		for (JsonObject candidate : List.of(jem, jemSlim)) {
			Set<String> parts = new HashSet<>();
			Set<String> ids = new HashSet<>();
			collect(candidate.getAsJsonArray("models"), parts, ids);
			for (String carrier : REQUIRED_CARRIERS) {
				assertTrue(parts.contains(carrier), "missing carrier part " + carrier);
			}
			for (String id : REQUIRED_IDS) {
				assertTrue(ids.contains(id), "missing custom part id " + id);
			}
		}
	}

	@Test
	void everyBbmodelGroupHasACustomId() throws IOException {
		JsonObject bb = readJson(BBMODEL);
		Set<String> ids = new HashSet<>();
		collectIds(jem.getAsJsonArray("models"), ids);
		for (JsonElement element : bb.getAsJsonArray("groups")) {
			String name = element.getAsJsonObject().get("name").getAsString();
			if (name.equals("root")) {
				continue; // the root group is the transform carrier, not a part
			}
			assertTrue(ids.contains("hl_" + name),
					"bbmodel group " + name + " has no jem part id hl_" + name);
		}
	}

	@Test
	void allFiveClipsAndRequiredVarsPresent() {
		for (String var : REQUIRED_VARS) {
			assertTrue(jemText.contains(var), "jem never references " + var);
			assertTrue(jemSlimText.contains(var), "player_slim.jem never references " + var);
		}
	}

	@Test
	void nothingForbidden() {
		for (String text : List.of(jemText, jemSlimText)) {
			assertFalse(text.contains("emf_lab"), "emf_lab expression leaked into the jem");
			assertFalse(text.contains("\"cloak\""), "cloak part must not be emitted");
			assertFalse(text.toLowerCase().contains("nan"), "NaN literal in jem");
		}
		// root-level texture is forbidden; part-level milk texture is required
		assertFalse(jem.has("texture"), "player.jem must not pin a root texture");
	}

	@Test
	void hiddenOverlayPartsAreBooleanHidden() {
		Set<String> hidden = new HashSet<>();
		for (JsonElement element : jem.getAsJsonArray("models")) {
			JsonObject part = element.getAsJsonObject();
			String name = part.has("part") ? part.get("part").getAsString() : "";
			if (HIDDEN_PARTS.contains(name)) {
				hidden.add(name);
				boolean booleanHidden = false;
				for (JsonElement anim : part.getAsJsonArray("animations")) {
					String expr = anim.getAsJsonObject().get(name + ".visible").getAsString();
					if (expr.replace(" ", "").equals("1=0")) {
						booleanHidden = true;
					}
				}
				assertTrue(booleanHidden, name + ".visible is not the boolean 1=0 form");
			}
		}
		assertTrue(hidden.containsAll(HIDDEN_PARTS), "missing hidden overlay parts: " + hidden);
	}

	@Test
	void visibleChannelsAreBooleanValuedOnly() {
		for (JsonObject candidate : List.of(jem, jemSlim)) {
			checkVisibleChannels(candidate.getAsJsonArray("models"));
		}
	}

	private static void checkVisibleChannels(JsonElement parts) {
		for (JsonElement element : parts.getAsJsonArray()) {
			JsonObject part = element.getAsJsonObject();
			if (part.has("animations")) {
				for (JsonElement anim : part.getAsJsonArray("animations")) {
					for (String key : anim.getAsJsonObject().keySet()) {
						String channel = key.substring(key.lastIndexOf('.') + 1);
						if (!channel.equals("visible") && !channel.equals("visible_boxes")) {
							continue;
						}
						String expr = anim.getAsJsonObject().get(key).getAsString().replace(" ", "");
						assertFalse(NUMBER.matcher(expr).matches(),
								key + " is a bare numeric literal: " + expr);
						assertTrue(expr.contains("=0") || expr.contains("=1")
										|| expr.contains("is_first_person_hand")
										|| expr.contains("true") || expr.contains("false"),
								key + " is not a boolean expression: " + expr);
					}
				}
			}
			if (part.has("submodels")) {
				checkVisibleChannels(part.get("submodels"));
			}
		}
	}

	private static JsonObject readJson(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private static void collect(JsonElement parts, Set<String> partNames, Set<String> ids) {
		collectParts(parts, partNames);
		collectIds(parts, ids);
	}

	private static void collectParts(JsonElement parts, Set<String> partNames) {
		for (JsonElement element : parts.getAsJsonArray()) {
			JsonObject part = element.getAsJsonObject();
			if (part.has("part")) {
				partNames.add(part.get("part").getAsString());
			}
			if (part.has("submodels")) {
				collectParts(part.get("submodels"), partNames);
			}
		}
	}

	private static void collectIds(JsonElement parts, Set<String> ids) {
		for (JsonElement element : parts.getAsJsonArray()) {
			JsonObject part = element.getAsJsonObject();
			if (part.has("id")) {
				ids.add(part.get("id").getAsString());
			}
			if (part.has("submodels")) {
				collectIds(part.get("submodels"), ids);
			}
		}
	}
}
