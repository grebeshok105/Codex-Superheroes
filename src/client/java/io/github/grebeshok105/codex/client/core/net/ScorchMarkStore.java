package io.github.grebeshok105.codex.client.core.net;

import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.core.net.ScorchMark;

import java.util.Arrays;
import java.util.List;

/**
 * Client-side mirror of the server's laser scorch marks. Hero-agnostic storage:
 * {@link #receive} appends broadcast marks, a {@code reset} batch (first chunk
 * of a join/dimension-change sync) clears the store first, and disconnect drops
 * everything via {@link ClientSessionState}.
 *
 * <p>Storage is a fixed column layout written through a ring cursor, so appends
 * are O(1) and nothing is allocated per frame — renderers index the raw
 * columns directly. Capacity mirrors {@code LaserScorchData.MAX_MARKS} (that
 * hero class may not be referenced from client.core).
 */
public final class ScorchMarkStore {
	public static final int CAPACITY = 2048;

	private static final long[] posLongs = new long[CAPACITY];
	private static final byte[] faces = new byte[CAPACITY];
	private static final float[] us = new float[CAPACITY];
	private static final float[] vs = new float[CAPACITY];
	private static final float[] sizes = new float[CAPACITY];
	private static final int[] rots = new int[CAPACITY];
	private static final boolean[] live = new boolean[CAPACITY];
	private static int next;

	static {
		ClientSessionState.register(ScorchMarkStore::clear);
	}

	private ScorchMarkStore() {
	}

	/** Applies a scorch-mark batch on the render thread. */
	public static void receive(List<ScorchMark> marks, boolean reset) {
		if (reset) {
			clear();
		}
		for (ScorchMark mark : marks) {
			posLongs[next] = mark.pos().asLong();
			faces[next] = mark.face();
			us[next] = mark.u();
			vs[next] = mark.v();
			sizes[next] = mark.size();
			rots[next] = mark.rot();
			live[next] = true;
			next = (next + 1) % CAPACITY;
		}
	}

	public static void clear() {
		Arrays.fill(live, false);
		next = 0;
	}

	public static boolean live(int i) {
		return live[i];
	}

	public static long posLong(int i) {
		return posLongs[i];
	}

	public static byte face(int i) {
		return faces[i];
	}

	public static float u(int i) {
		return us[i];
	}

	public static float v(int i) {
		return vs[i];
	}

	public static float size(int i) {
		return sizes[i];
	}

	public static int rot(int i) {
		return rots[i];
	}
}
