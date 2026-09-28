package io.github.grebeshok105.codex.client.core.anim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link BedrockAnimationParser} against the Bedrock {@code .animation.json} shapes OMP ships. */
class BedrockAnimationParserTest {

	@Test
	void parsesLengthLoopAndBones() throws IOException {
		JsonObject root;
		try (InputStream in = getClass().getResourceAsStream("/anim/sample.animation.json")) {
			assertNotNull(in, "sample.animation.json fixture missing");
			root = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
		}
		List<String> warnings = new ArrayList<>();
		List<AnimationClip> clips = BedrockAnimationParser.parse(root, warnings::add);

		assertEquals(1, clips.size(), "expected exactly one clip, warnings: " + warnings);
		AnimationClip clip = clips.get(0);
		assertEquals(ResourceLocation.fromNamespaceAndPath("superheroes", "homelander/clap"), clip.id());
		assertEquals(0.6f, clip.lengthSeconds(), 1e-4);
		assertEquals(AnimationClip.Loop.OFF, clip.loop());
		assertTrue(clip.bones().containsKey("right_arm"), "right_arm track missing");
		assertTrue(clip.bones().containsKey("left_arm"), "left_arm track missing");
		assertEquals(0.12f, clip.eventTimes().getOrDefault("contact", -1f), 1e-4);
		assertTrue(warnings.isEmpty(), "unexpected warnings: " + warnings);
	}

	@Test
	void molangKeyframeRejectedNotThrown() {
		JsonObject root = JsonParser.parseString("""
				{"animations": {"animation.superheroes.homelander.bad": {
					"animation_length": 1.0,
					"bones": {"right_arm": {"rotation": {"0.0": ["math.sin(q.anim_time)", 0, 0]}}}
				}}}
				""").getAsJsonObject();
		List<String> warnings = new ArrayList<>();
		List<AnimationClip> clips = BedrockAnimationParser.parse(root, warnings::add);

		assertTrue(clips.isEmpty(), "molang clip must be skipped, not parsed");
		assertEquals(1, warnings.size(), "expected exactly one warning, got: " + warnings);
	}

	@Test
	void unknownBoneIgnored() {
		JsonObject root = JsonParser.parseString("""
				{"animations": {"animation.superheroes.homelander.tail": {
					"animation_length": 1.0,
					"bones": {
						"tail": {"rotation": {"0.0": {"vector": [1, 2, 3]}}},
						"head": {"rotation": {"0.0": {"vector": [0, 5, 0]}}}
					}
				}}}
				""").getAsJsonObject();
		List<String> warnings = new ArrayList<>();
		List<AnimationClip> clips = BedrockAnimationParser.parse(root, warnings::add);

		assertEquals(1, clips.size(), "clip must survive an unknown bone");
		assertFalse(clips.get(0).bones().containsKey("tail"), "unknown bone must be ignored");
		assertTrue(clips.get(0).bones().containsKey("head"), "known bone must survive");
		assertEquals(1, warnings.size(), "expected exactly one warning, got: " + warnings);
	}

	@Test
	void holdOnLastFrameParsesAsNonLoop() {
		JsonObject root = JsonParser.parseString("""
				{"animations": {"animation.superheroes.homelander.hold": {
					"animation_length": 1.0,
					"loop": "hold_on_last_frame",
					"bones": {"head": {"rotation": {"0.0": {"vector": [0, 5, 0]}}}}
				}}}
				""").getAsJsonObject();
		List<AnimationClip> clips = BedrockAnimationParser.parse(root, w -> {
			throw new AssertionError("unexpected warning: " + w);
		});

		assertEquals(1, clips.size());
		assertEquals(AnimationClip.Loop.HOLD, clips.get(0).loop());
	}
}
