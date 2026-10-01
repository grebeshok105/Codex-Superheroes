package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;

/**
 * Client factories for Regulus's one-shot VFX events ({@code RegulusVfxIds}).
 * The {@code regulus/anim/<clip>} ids are clip-starts for the authored EMF
 * animation pack — the clip states themselves are owned by the EMF integration
 * (Task 9); until then these factories return a one-tick {@code DoneFx}.
 * Effect ids like {@code debris_impact} carry the burst origin/target that
 * Task 9's Veil effect consumes.
 */
public final class RegulusFx {
	private RegulusFx() {
	}

	public static void register(HeroClientContext ctx) {
		ctx.vfx(RegulusVfxIds.ANIM_DEBRIS_KICK, RegulusFx::clipOnly);
		ctx.vfx(RegulusVfxIds.DEBRIS_IMPACT, RegulusFx::clipOnly);
	}

	/**
	 * Clip-start events with no own visual yet: the EMF clip states land with the
	 * Veil/EMF task — until then a one-tick done effect keeps the contract alive.
	 * Also used for {@code evangelium_major} (the authored hit-frame event).
	 */
	public static VfxEffect clipOnly(VfxSpawn spawn) {
		return new DoneFx();
	}

	/** Ends after one tick — the effect's real work happened in the factory. */
	private static final class DoneFx implements VfxEffect {
		private boolean done;

		@Override
		public void tick() {
			done = true;
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return done;
		}
	}
}
