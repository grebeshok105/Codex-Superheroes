package io.github.grebeshok105.codex.client.hero.homelander.emf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Translates {@link EmfClip} tracks into EMF expression lines
 * ({@code "bone.channel" → expression text}) so EMF itself does all keyframe
 * evaluation — the "second engine" the spec forbids never exists.
 *
 * <p>Each channel expression is the weight-summed contribution over every
 * clip animating it: {@code Σ var.<clip>_w · keyframe|keyframeloop(var.<clip>_t, lits…)}.
 * Literals are baked to vanilla space at build time
 * ({@link EmfModelConversion}); a channel whose frames are all equal folds to
 * a single literal.
 *
 * <p>Var contract (see {@code docs/design/homelander-emf-animations.md}):
 * {@code var.<clip>_w} blend weight ∈ [0,1], {@code var.<clip>_t} clip time in
 * 60-fps frames, {@code var.lean_pitch}/{@code var.lean_roll} (radians) and
 * {@code var.lean_y} (pixels) — lean vars are added onto the {@code root}
 * bone's {@code rx}/{@code rz}/{@code ty} lines respectively.
 */
public final class EmfExpressionBuilder {
	public static final String LEAN_PITCH_VAR = "var.lean_pitch";
	public static final String LEAN_ROLL_VAR = "var.lean_roll";
	public static final String LEAN_Y_VAR = "var.lean_y";

	public static final String ROOT_BONE = "root";

	public record Line(String animKey, String expression) {
	}

	public record Built(List<Line> lines, Set<String> varNames) {
	}

	public static Built build(EmfModelData model, Collection<EmfClip> clips) {
		Map<String, EMFChannel> animated = new LinkedHashMap<>();
		for (EmfClip clip : clips) {
			for (Map.Entry<String, Map<EmfClip.Channel, float[][]>> boneEntry : clip.tracks().entrySet()) {
				String bone = boneEntry.getKey();
				if (!model.bones().containsKey(bone)) {
					continue;
				}
				for (Map.Entry<EmfClip.Channel, float[][]> channelEntry : boneEntry.getValue().entrySet()) {
					animated.computeIfAbsent(bone, k -> new EMFChannel()).add(clip, channelEntry.getKey(), channelEntry.getValue());
				}
			}
		}
		List<Line> lines = new ArrayList<>();
		Set<String> varNames = new TreeSet<>();
		for (EmfClip clip : clips) {
			varNames.add(weightVar(clip));
			varNames.add(timeVar(clip));
		}
		varNames.add(LEAN_PITCH_VAR);
		varNames.add(LEAN_ROLL_VAR);
		varNames.add(LEAN_Y_VAR);

		for (Map.Entry<String, EMFChannel> entry : animated.entrySet()) {
			String bone = entry.getKey();
			boolean prop = model.propBones().contains(bone);
			float[] rest = restOffset(model, bone);
			EMFChannel channels = entry.getValue();
			for (int comp = 0; comp < 3; comp++) {
				final int c = comp;
				if (!channels.rotation.isEmpty()) {
					String expr = blend(channels.rotation, c,
							frames -> EmfModelConversion.rotationRad(
									frames[0], frames[1], frames[2])[c]);
					if (ROOT_BONE.equals(bone) && c == 0) {
						expr = LEAN_PITCH_VAR + " + " + expr;
					}
					if (ROOT_BONE.equals(bone) && c == 2) {
						expr = LEAN_ROLL_VAR + " + " + expr;
					}
					lines.add(new Line(bone + "." + "r" + "xyz".charAt(c), expr));
				}
				if (!channels.position.isEmpty()) {
					String expr = fmt(rest[c]) + " + " + blend(channels.position, c,
							frames -> EmfModelConversion.positionDelta(
									frames[0], frames[1], frames[2])[c]);
					if (ROOT_BONE.equals(bone) && c == 1) {
						expr = expr + " + " + LEAN_Y_VAR;
					}
					lines.add(new Line(bone + "." + "t" + "xyz".charAt(c), expr));
				}
				if (!channels.scale.isEmpty()) {
					float baseline = prop ? 0f : 1f;
					String expr = fmt(baseline) + " + " + blend(channels.scale, c,
							frames -> frames[c] - baseline);
					lines.add(new Line(bone + "." + "s" + "xyz".charAt(c), expr));
				}
			}
		}
		// Lean vars need lines even when no clip animates the root channels.
		// (They always exist above for flight clips, but keep the contract for
		// any clip set.)
		boolean hasRootRx = lines.stream().anyMatch(l -> l.animKey().equals("root.rx"));
		boolean hasRootRz = lines.stream().anyMatch(l -> l.animKey().equals("root.rz"));
		boolean hasRootTy = lines.stream().anyMatch(l -> l.animKey().equals("root.ty"));
		if (!hasRootRx) {
			lines.add(new Line("root.rx", LEAN_PITCH_VAR));
		}
		if (!hasRootRz) {
			lines.add(new Line("root.rz", LEAN_ROLL_VAR));
		}
		if (!hasRootTy) {
			float rest = restOffset(model, ROOT_BONE)[1];
			lines.add(new Line("root.ty", fmt(rest) + " + " + LEAN_Y_VAR));
		}
		return new Built(lines, varNames);
	}

	public static String weightVar(EmfClip clip) {
		return "var." + clip.varName() + "_w";
	}

	public static String timeVar(EmfClip clip) {
		return "var." + clip.varName() + "_t";
	}

	public static String weightVarName(String clipName) {
		return "var." + clipName + "_w";
	}

	public static String timeVarName(String clipName) {
		return "var." + clipName + "_t";
	}

	/** The rest-position offset (vanilla space) baked into {@code t*} lines. */
	private static float[] restOffset(EmfModelData model, String boneName) {
		EmfModelData.Bone bone = model.bones().get(boneName);
		if (bone.parent() == null) {
			return EmfModelConversion.pivot(bone.pivot());
		}
		return EmfModelConversion.relativePivot(bone.pivot(),
				model.bones().get(bone.parent()).pivot());
	}

	private interface Comp {
		float apply(float[] frame);
	}

	/**
	 * {@code Σ weight · keyframe(time, lits…)} for one component of one channel
	 * across all contributing clips. Constant frames of non-loop clips fold to
	 * {@code w·lit}; loop clips always emit {@code keyframeloop} so the time
	 * var keeps driving evaluation.
	 */
	private static String blend(List<ClipTrack> tracks, int comp, Comp convert) {
		StringBuilder sb = new StringBuilder();
		for (ClipTrack track : tracks) {
			float[][] frames = track.frames();
			float[] values = new float[frames.length];
			boolean constant = true;
			float first = convert.apply(frames[0]);
			for (int f = 0; f < frames.length; f++) {
				values[f] = convert.apply(frames[f]);
				if (values[f] != first) {
					constant = false;
				}
			}
			if (sb.length() > 0) {
				sb.append(" + ");
			}
			String weight = weightVar(track.clip());
			String time = timeVar(track.clip());
			if (constant && !track.clip().loop()) {
				sb.append(weight).append(" * ").append(fmt(values[0]));
			} else {
				sb.append(weight).append(" * ")
						.append(track.clip().loop() ? "keyframeloop" : "keyframe")
						.append('(').append(time);
				for (float v : values) {
					sb.append(", ").append(fmt(v));
				}
				sb.append(')');
			}
		}
		return sb.toString();
	}

	private static String fmt(float v) {
		if (v == (int) v) {
			return Integer.toString((int) v);
		}
		return Float.toString(v);
	}

	private record ClipTrack(EmfClip clip, float[][] frames) {
	}

	private static final class EMFChannel {
		private final List<ClipTrack> rotation = new ArrayList<>();
		private final List<ClipTrack> position = new ArrayList<>();
		private final List<ClipTrack> scale = new ArrayList<>();

		private void add(EmfClip clip, EmfClip.Channel channel, float[][] frames) {
			switch (channel) {
				case ROTATION -> rotation.add(new ClipTrack(clip, frames));
				case POSITION -> position.add(new ClipTrack(clip, frames));
				case SCALE -> scale.add(new ClipTrack(clip, frames));
			}
		}
	}
}
