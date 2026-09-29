package io.github.grebeshok105.codex.client.core.vfx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link VfxInstanceTable} — the Minecraft-free budget/lifecycle logic
 * {@code VfxRuntime} delegates to (stress scene: the cap must hold and the
 * oldest effect must be cancelled, never the frame).
 */
class VfxRuntimeBudgetTest {
	private static final class FakeEffect implements VfxEffect {
		int ticks;
		boolean cancelled;
		boolean done;

		@Override
		public void tick() {
			ticks++;
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return done;
		}

		@Override
		public void cancel() {
			cancelled = true;
		}
	}

	@Test
	void budgetEvictsOldestBeyondCap() {
		VfxInstanceTable table = new VfxInstanceTable(VfxRuntime.MAX_ACTIVE_EFFECTS);
		List<FakeEffect> effects = new ArrayList<>();
		for (int i = 0; i < VfxRuntime.MAX_ACTIVE_EFFECTS + 1; i++) {
			FakeEffect effect = new FakeEffect();
			effects.add(effect);
			table.add(effect);
		}
		assertEquals(VfxRuntime.MAX_ACTIVE_EFFECTS, table.size());
		assertTrue(effects.get(0).cancelled);
		assertFalse(effects.get(1).cancelled);
	}

	@Test
	void doneEffectsRemovedOnTick() {
		VfxInstanceTable table = new VfxInstanceTable(8);
		FakeEffect alive = new FakeEffect();
		FakeEffect dead = new FakeEffect();
		dead.done = true;
		table.add(alive);
		table.add(dead);

		table.tick();

		assertEquals(1, table.size());
		assertEquals(1, alive.ticks);
	}
}
