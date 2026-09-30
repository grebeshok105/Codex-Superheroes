package io.github.grebeshok105.codex.client.core.emf;

import io.github.grebeshok105.codex.client.ClientSessionState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/**
 * Fixed bounded slab that carries a rendered part's post-animation transform
 * ({@code x,y,z,xRot,yRot,zRot}) out of {@code PlayerRenderer.render} for
 * consumers that run outside the model pipeline (anchors, lasers). Filled at
 * the render tail — after EMF has run its animate() — so owned entities expose
 * the EMF-posed transform while vanilla entities keep their model pose.
 *
 * <p>64 slots, LRU eviction, zero per-call allocation: three flat arrays and
 * caller-supplied scratch outputs.
 */
public final class RenderedPoseCache {

	static final int CAPACITY = 64;
	private static final int PART_COUNT = 4;    // head, body, leftArm, rightArm
	private static final int PART_FLOATS = 6;   // x, y, z, xRot, yRot, zRot
	private static final int STRIDE = PART_COUNT * PART_FLOATS;
	private static final int EMPTY = Integer.MIN_VALUE;

	private static final int[] IDS = new int[CAPACITY];
	private static final long[] USED = new long[CAPACITY];
	private static final float[] DATA = new float[CAPACITY * STRIDE];
	private static int size;
	private static long clock;

	static {
		ClientSessionState.register(RenderedPoseCache::clearAll);
		// Snapshots are keyed by entity id inside one ClientLevel; a level swap
		// (dimension change, respawn, rejoin) must not leak poses across.
		ClientSessionState.registerLevelReset(RenderedPoseCache::clearAll);
	}

	private RenderedPoseCache() {
	}

	/** Snapshot the four presented parts of a rendered player model. */
	public static void capture(int entityId, PlayerModel<?> model) {
		int base = slot(entityId) * STRIDE;
		write(model.head, base);
		write(model.body, base + PART_FLOATS);
		write(model.leftArm, base + 2 * PART_FLOATS);
		write(model.rightArm, base + 3 * PART_FLOATS);
	}

	private static void write(ModelPart part, int base) {
		DATA[base] = part.x;
		DATA[base + 1] = part.y;
		DATA[base + 2] = part.z;
		DATA[base + 3] = part.xRot;
		DATA[base + 4] = part.yRot;
		DATA[base + 5] = part.zRot;
	}

	/** Head pose into {@code out[0..6]} as x,y,z,xRot,yRot,zRot; false if absent. */
	public static boolean headPose(int entityId, float[] out) {
		int slot = find(entityId);
		if (slot < 0) {
			return false;
		}
		USED[slot] = ++clock;
		System.arraycopy(DATA, slot * STRIDE, out, 0, PART_FLOATS);
		return true;
	}

	/** Head offset in model pixels; false if absent. */
	public static boolean headOffsetPx(int entityId, Vector3f out) {
		int slot = find(entityId);
		if (slot < 0) {
			return false;
		}
		USED[slot] = ++clock;
		out.set(DATA[slot * STRIDE], DATA[slot * STRIDE + 1], DATA[slot * STRIDE + 2]);
		return true;
	}

	/** Head euler rotation in radians (xRot, yRot, zRot); false if absent. */
	public static boolean headRotationRad(int entityId, Vector3f out) {
		int slot = find(entityId);
		if (slot < 0) {
			return false;
		}
		USED[slot] = ++clock;
		out.set(DATA[slot * STRIDE + 3], DATA[slot * STRIDE + 4], DATA[slot * STRIDE + 5]);
		return true;
	}

	public static void clear(int entityId) {
		for (int i = 0; i < size; i++) {
			if (IDS[i] == entityId) {
				int last = --size;
				IDS[i] = IDS[last];
				USED[i] = USED[last];
				System.arraycopy(DATA, last * STRIDE, DATA, i * STRIDE, STRIDE);
				IDS[last] = EMPTY;
				return;
			}
		}
	}

	public static void clearAll() {
		size = 0;
		for (int i = 0; i < CAPACITY; i++) {
			IDS[i] = EMPTY;
			USED[i] = 0;
		}
	}

	private static int slot(int entityId) {
		int found = find(entityId);
		if (found >= 0) {
			USED[found] = ++clock;
			return found;
		}
		if (size < CAPACITY) {
			IDS[size] = entityId;
			USED[size] = ++clock;
			return size++;
		}
		int oldest = 0;
		for (int i = 1; i < CAPACITY; i++) {
			if (USED[i] < USED[oldest]) {
				oldest = i;
			}
		}
		IDS[oldest] = entityId;
		USED[oldest] = ++clock;
		return oldest;
	}

	private static int find(int entityId) {
		for (int i = 0; i < size; i++) {
			if (IDS[i] == entityId) {
				return i;
			}
		}
		return -1;
	}
}
