package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.core.anim.AnimationClip;
import io.github.grebeshok105.codex.client.core.anim.BoneTrack;
import io.github.grebeshok105.codex.client.core.anim.Keyframes;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ports a legacy Bedrock {@link AnimationClip} (6 vanilla bones, arbitrary
 * keyframe times/easings) into an {@link EmfClip} by resampling at the EMF
 * clip rate of 60 fps. The only runtime-side keyframe machinery involved is
 * {@link Keyframes#sample} — the resampling the spec allows — the produced
 * clip then runs entirely through the same generated EMF expressions.
 *
 * <p>Coordinate conversion: the old {@code PlayerPoseApplier} mapped bedrock
 * → vanilla as rotation {@code (−x,−y,+z)°} / position {@code (−x,−y,+z) px}.
 * Our clip pipeline applies {@link EmfModelConversion} to every literal, so
 * the ported frames are pre-converted bedrock → <em>Blockbench space</em>:
 * rotation {@code (x, y, −z)}, position {@code (−x, +y, +z)} — making the
 * composed transform equal the old applier's output.
 */
public final class LegacyClipPort {
	public static final int SAMPLE_FPS = 60;

	private LegacyClipPort() {
	}

	public static EmfClip port(AnimationClip clip) {
		int frames = Math.max(2, (int) Math.ceil(clip.lengthSeconds() * SAMPLE_FPS) + 1);
		Map<String, Map<EmfClip.Channel, float[][]>> tracks = new LinkedHashMap<>();
		for (Map.Entry<String, BoneTrack> bone : clip.bones().entrySet()) {
			BoneTrack track = bone.getValue();
			Map<EmfClip.Channel, float[][]> channels = new EnumMap<>(EmfClip.Channel.class);
			if (track.rotation() != null && !track.rotation().isEmpty()) {
				float[][] rot = new float[frames][3];
				for (int f = 0; f < frames; f++) {
					Vector3f v = track.rotation().sample(f / (float) SAMPLE_FPS);
					rot[f][0] = v.x;
					rot[f][1] = v.y;
					rot[f][2] = -v.z;
				}
				channels.put(EmfClip.Channel.ROTATION, rot);
			}
			if (track.position() != null && !track.position().isEmpty()) {
				float[][] pos = new float[frames][3];
				for (int f = 0; f < frames; f++) {
					Vector3f v = track.position().sample(f / (float) SAMPLE_FPS);
					pos[f][0] = -v.x;
					pos[f][1] = v.y;
					pos[f][2] = v.z;
				}
				channels.put(EmfClip.Channel.POSITION, pos);
			}
			if (!channels.isEmpty()) {
				tracks.put(bone.getKey(), channels);
			}
		}
		return new EmfClip(clip.id(), frames, SAMPLE_FPS,
				clip.loop() == AnimationClip.Loop.WRAP, false, tracks);
	}
}
