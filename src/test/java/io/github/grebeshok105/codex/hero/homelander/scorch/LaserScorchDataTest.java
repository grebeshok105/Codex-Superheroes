package io.github.grebeshok105.codex.hero.homelander.scorch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.grebeshok105.codex.core.net.ScorchMark;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class LaserScorchDataTest {
	private static final UUID CASTER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	private static final UUID CASTER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

	private static ScorchMark mark(int i) {
		return new ScorchMark(new BlockPos(i, 64, 0), (byte) 1, 0.5f, 0.5f, 0.3f, i % 360);
	}

	private static List<ScorchMark> collect(LaserScorchData data) {
		List<ScorchMark> out = new ArrayList<>();
		data.forEachChronological(out::add);
		return out;
	}

	@Test
	void ringBufferOverwritesOldest() {
		LaserScorchData data = new LaserScorchData();
		for (int i = 0; i < LaserScorchData.MAX_MARKS + 5; i++) {
			data.insert(mark(i));
		}
		assertEquals(LaserScorchData.MAX_MARKS, data.size());
		List<ScorchMark> marks = collect(data);
		assertEquals(LaserScorchData.MAX_MARKS, marks.size());
		// oldest five were evicted; iteration stays chronological
		assertEquals(5, marks.get(0).rot());
		assertEquals((LaserScorchData.MAX_MARKS + 4) % 360, marks.get(marks.size() - 1).rot());
	}

	@Test
	void spacingRejectsMarksTooCloseToThePreviousOne() {
		LaserScorchData data = new LaserScorchData();
		assertTrue(data.accept(CASTER_A, mark(0), new Vec3(0, 0, 0), 1000));
		assertFalse(data.accept(CASTER_A, mark(1), new Vec3(0.2, 0, 0), 1001),
				"0.2 < 0.35 spacing must reject");
		assertTrue(data.accept(CASTER_A, mark(2), new Vec3(0.4, 0, 0), 1002),
				"0.4 >= 0.35 spacing accepts");
		assertEquals(2, data.size());
	}

	@Test
	void rateLimitCapsMarksPerSecond() {
		LaserScorchData data = new LaserScorchData();
		long t0 = 5000;
		for (int i = 0; i < LaserScorchData.MAX_PER_SECOND; i++) {
			assertTrue(data.accept(CASTER_A, mark(i), new Vec3(i * 0.5, 0, 0), t0 + i),
					"accept " + i);
		}
		assertFalse(data.accept(CASTER_A, mark(99), new Vec3(10, 0, 0), t0 + LaserScorchData.MAX_PER_SECOND),
				"7th mark inside the same second is rejected");
		// window rolls: a second later another mark lands
		assertTrue(data.accept(CASTER_A, mark(100), new Vec3(10, 0, 0), t0 + 20));
	}

	@Test
	void budgetsArePerCaster() {
		LaserScorchData data = new LaserScorchData();
		assertTrue(data.accept(CASTER_A, mark(0), new Vec3(0, 0, 0), 0));
		assertFalse(data.accept(CASTER_A, mark(1), new Vec3(0.1, 0, 0), 1));
		assertTrue(data.accept(CASTER_B, mark(1), new Vec3(0.1, 0, 0), 1),
				"a different caster is not bound by A's spacing");
	}

	@Test
	void rejectedMarkDoesNotConsumeRateBudget() {
		LaserScorchData data = new LaserScorchData();
		assertTrue(data.accept(CASTER_A, mark(0), new Vec3(0, 0, 0), 0));
		// spacing-rejected hits don't count toward the 6/s cap
		for (int i = 1; i <= 20; i++) {
			assertFalse(data.accept(CASTER_A, mark(i), new Vec3(0.05, 0, 0), i));
		}
		for (int i = 0; i < LaserScorchData.MAX_PER_SECOND; i++) {
			assertTrue(data.accept(CASTER_A, mark(50 + i), new Vec3(1 + i, 0, 0), 30 + i),
					"accept " + i + " — rejected spacing hits left the budget open");
		}
		assertFalse(data.accept(CASTER_A, mark(99), new Vec3(20, 0, 0), 36), "cap still applies");
	}

	@Test
	void saveLoadRoundTripsAllMarks() {
		LaserScorchData data = new LaserScorchData();
		for (int i = 0; i < 40; i++) {
			data.insert(mark(i));
		}
		CompoundTag tag = data.save(new CompoundTag(), null);
		LaserScorchData restored = LaserScorchData.load(tag, null);
		assertEquals(collect(data), collect(restored));
	}

	@Test
	void saveLoadTruncatesToRingCapacity() {
		LaserScorchData data = new LaserScorchData();
		for (int i = 0; i < LaserScorchData.MAX_MARKS; i++) {
			data.insert(mark(i));
		}
		CompoundTag tag = data.save(new CompoundTag(), null);
		LaserScorchData restored = LaserScorchData.load(tag, null);
		assertEquals(LaserScorchData.MAX_MARKS, restored.size());
	}
}
