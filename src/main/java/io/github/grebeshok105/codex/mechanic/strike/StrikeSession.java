package io.github.grebeshok105.codex.mechanic.strike;

/**
 * A queued delayed strike owned by one caster. Modules store whatever bookkeeping their
 * windup needs (target position, lock position, variant); the queue only needs the tick
 * the strike resolves on, in the owner's level game-time.
 */
public interface StrikeSession {
	/** Game-time tick at which {@link QueuedStrikes.Handlers#impact} fires and the entry drops. */
	long impactTick();
}
