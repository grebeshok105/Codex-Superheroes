package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;

/**
 * Client factories for Regulus's Visual Core events. The two debris-kick ids ship
 * as no-op one-tick stubs for now: the cast-id event ({@code ANIM_DEBRIS_KICK})
 * names the authored clip Task 9's EMF binding will start, and the impact event
 * carries the burst origin/target Task 9's Veil effect consumes. Registering them
 * here keeps the event contract live without inventing placeholder visuals.
 */
public final class RegulusFx {
	private RegulusFx() {
	}

	public static void register(HeroClientContext ctx) {
		ctx.vfx(RegulusVfxIds.ANIM_DEBRIS_KICK, RegulusFx::debrisKickAnim);
		ctx.vfx(RegulusVfxIds.DEBRIS_IMPACT, RegulusFx::debrisImpact);
	}

	private static VfxEffect debrisKickAnim(VfxSpawn spawn) {
		return new DoneFx();
	}

	private static VfxEffect debrisImpact(VfxSpawn spawn) {
		return new DoneFx();
	}

	/** Ends after one tick — the event's real work happened in the factory. */
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
