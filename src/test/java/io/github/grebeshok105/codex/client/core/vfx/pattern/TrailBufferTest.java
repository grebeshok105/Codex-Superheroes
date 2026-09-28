package io.github.grebeshok105.codex.client.core.vfx.pattern;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Drives {@link TrailBuffer} ring semantics: newest-first reads, oldest overwritten. */
class TrailBufferTest {
	@Test
	void getZeroIsNewest() {
		TrailBuffer buffer = new TrailBuffer(3);
		buffer.push(new Vec3(1, 0, 0));
		buffer.push(new Vec3(2, 0, 0));
		buffer.push(new Vec3(3, 0, 0));
		assertEquals(3, buffer.size());
		assertEquals(new Vec3(3, 0, 0), buffer.get(0));
		assertEquals(new Vec3(2, 0, 0), buffer.get(1));
		assertEquals(new Vec3(1, 0, 0), buffer.get(2));
	}

	@Test
	void overwritesOldest() {
		TrailBuffer buffer = new TrailBuffer(3);
		for (int i = 1; i <= 5; i++) {
			buffer.push(new Vec3(i, 0, 0));
		}
		assertEquals(3, buffer.size());
		assertEquals(new Vec3(5, 0, 0), buffer.get(0));
		assertEquals(new Vec3(4, 0, 0), buffer.get(1));
		assertEquals(new Vec3(3, 0, 0), buffer.get(2));
	}
}
