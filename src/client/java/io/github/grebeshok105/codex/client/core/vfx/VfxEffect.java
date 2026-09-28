package io.github.grebeshok105.codex.client.core.vfx;

/**
 * A live client-side visual effect driven by {@link VfxRuntime}: one game tick
 * per client tick, one {@link #render} call per frame on the AFTER_TRANSLUCENT
 * pass. {@link #done()} ends the effect; {@link #cancel()} is the budget
 * eviction path and must stop the effect immediately.
 */
public interface VfxEffect {
	void tick();

	/**
	 * Called inside the live-effect deque iteration in {@code VfxRuntime.render} —
	 * implementations must not call {@code VfxRuntime.spawn}/{@code channel} here.
	 */
	void render(VfxRenderContext ctx);

	boolean done();

	/** Hard stop when the runtime evicts this effect over budget. */
	default void cancel() {
	}
}
