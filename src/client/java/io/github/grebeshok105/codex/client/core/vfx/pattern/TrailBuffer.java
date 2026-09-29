package io.github.grebeshok105.codex.client.core.vfx.pattern;

import net.minecraft.world.phys.Vec3;

/**
 * Fixed-capacity ring of trail points. {@code get(0)} is the newest pushed
 * point; once full, pushes overwrite the oldest entry.
 */
public final class TrailBuffer {
	private final Vec3[] points;
	private int head;
	private int size;

	public TrailBuffer(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		points = new Vec3[capacity];
	}

	public void push(Vec3 point) {
		points[head] = point;
		head = (head + 1) % points.length;
		if (size < points.length) {
			size++;
		}
	}

	public int size() {
		return size;
	}

	/** {@code get(0)} returns the newest point, {@code get(size()-1)} the oldest retained. */
	public Vec3 get(int i) {
		if (i < 0 || i >= size) {
			throw new IndexOutOfBoundsException(i);
		}
		return points[(head - 1 - i + points.length) % points.length];
	}
}
