package io.github.grebeshok105.codex.hero.homelander;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.grebeshok105.codex.hero.homelander.ability.HandClapAbility;
import io.github.grebeshok105.codex.hero.homelander.item.MilkBottleItem;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timing contract between the authored EMF clips and the gameplay constants
 * that must stay aligned with them:
 *
 * <ul>
 *   <li>{@link HandClapAbility#CONTACT_TICKS} — the clap's hands-meet frame,
 *   measured as the peak of the summed per-frame rotation speed of both arms
 *   and both forearms in {@code hand_clap.json} (the swing peaks then stops
 *   dead at contact).</li>
 *   <li>{@link MilkBottleItem#DRINK_TICKS} — the milk sip moment, measured as
 *   the midpoint of the window where {@code mouth_open} scale and the
 *   {@code right_arm} raise both sit at ~95% of their max in
 *   {@code milk_drink.json} (bottle at face, mouth fully open).</li>
 * </ul>
 *
 * Both constants live on the gameplay classes; the measurements live here so
 * a regenerated clip that shifts the beats fails the build instead of
 * silently desyncing damage/FX from the animation.
 */
class HomelanderClipTimingTest {
	private static final Path CLIP_DIR = Path.of("").toAbsolutePath()
			.resolve("src/main/resources/assets/superheroes/emf/homelander");
	private static final String[] CLAP_ARM_BONES = {
			"right_arm", "left_arm", "right_forearm", "left_forearm"};
	private static final int FPS = 60;
	private static final int TPS = 20;

	@Test
	void handClapContactTickMatchesArmSpeedPeak() throws IOException {
		JsonObject clip = clip("hand_clap");
		assertEquals(FPS, clip.get("sample_fps").getAsInt(), "contract: 60 fps clips");

		int frameCount = clip.get("frame_count").getAsInt();
		float[][][] rotations = new float[CLAP_ARM_BONES.length][][];
		for (int b = 0; b < CLAP_ARM_BONES.length; b++) {
			rotations[b] = channel(clip, CLAP_ARM_BONES[b], "rotation");
			assertEquals(frameCount, rotations[b].length,
					"bone " + CLAP_ARM_BONES[b] + " covers the whole clip");
		}

		// Contact = the transition with the highest summed arm angular speed:
		// the swing accelerates into the clap and stops dead when hands meet.
		int contactFrame = -1;
		double peak = -1;
		for (int i = 1; i < frameCount; i++) {
			double speed = 0;
			for (float[][] rot : rotations) {
				speed += Math.sqrt(sq(rot[i][0] - rot[i - 1][0])
						+ sq(rot[i][1] - rot[i - 1][1]) + sq(rot[i][2] - rot[i - 1][2]));
			}
			if (speed > peak) {
				peak = speed;
				contactFrame = i;
			}
		}

		int measuredTicks = Math.round(contactFrame * (float) TPS / FPS);
		assertEquals(HandClapAbility.CONTACT_TICKS, measuredTicks,
				"clap contact at clip frame " + contactFrame + " (t="
						+ (contactFrame / (float) FPS) + "s) must equal CONTACT_TICKS");
	}

	@Test
	void milkDrinkSipTickMatchesOpenMouthPlateau() throws IOException {
		JsonObject clip = clip("milk_drink");
		assertEquals(FPS, clip.get("sample_fps").getAsInt(), "contract: 60 fps clips");

		int frameCount = clip.get("frame_count").getAsInt();
		float[][] mouth = channel(clip, "mouth_open", "scale");
		float[][] rightArm = channel(clip, "right_arm", "rotation");
		assertEquals(frameCount, mouth.length, "mouth_open covers the whole clip");
		assertEquals(frameCount, rightArm.length, "right_arm covers the whole clip");

		double mouthMax = 0;
		double armMax = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < frameCount; i++) {
			mouthMax = Math.max(mouthMax,
					Math.sqrt(sq(mouth[i][0]) + sq(mouth[i][1]) + sq(mouth[i][2])));
			armMax = Math.max(armMax, rightArm[i][0]);
		}
		int first = -1;
		int last = -1;
		for (int i = 0; i < frameCount; i++) {
			double mouthScale = Math.sqrt(sq(mouth[i][0]) + sq(mouth[i][1]) + sq(mouth[i][2]));
			if (mouthScale >= 0.95 * mouthMax && rightArm[i][0] >= 0.95 * armMax) {
				if (first < 0) {
					first = i;
				}
				last = i;
			}
		}
		assertTrue(first >= 0, "a fully-open sip plateau exists in milk_drink.json");
		int sipFrame = Math.round((first + last) / 2f);

		int measuredTicks = Math.round(sipFrame * (float) TPS / FPS);
		assertEquals(MilkBottleItem.DRINK_TICKS, measuredTicks,
				"milk sip at clip frame " + sipFrame + " (midpoint of [" + first + ","
						+ last + "], t=" + (sipFrame / (float) FPS) + "s) must equal DRINK_TICKS");
	}

	private static JsonObject clip(String name) throws IOException {
		try (Reader reader = Files.newBufferedReader(CLIP_DIR.resolve(name + ".json"))) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private static float[][] channel(JsonObject clip, String bone, String channel) {
		JsonArray frames = clip.getAsJsonObject("bones").getAsJsonObject(bone)
				.getAsJsonArray(channel);
		float[][] values = new float[frames.size()][3];
		for (int i = 0; i < frames.size(); i++) {
			JsonArray v = frames.get(i).getAsJsonArray();
			values[i][0] = v.get(0).getAsFloat();
			values[i][1] = v.get(1).getAsFloat();
			values[i][2] = v.get(2).getAsFloat();
		}
		return values;
	}

	private static double sq(double v) {
		return v * v;
	}
}
