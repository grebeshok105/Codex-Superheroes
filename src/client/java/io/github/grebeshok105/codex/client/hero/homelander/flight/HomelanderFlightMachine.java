package io.github.grebeshok105.codex.client.hero.homelander.flight;

/**
 * The Homelander flight presentation state machine — a pure transition table
 * over the server's {@code FlightPhase} plus the client-side forward-speed
 * check. No Minecraft types: the driver ({@code HomelanderFlightDriver})
 * translates entity state into {@link Input} and weight targets out of
 * {@link Phase}.
 *
 * <p>Semantics from {@code docs/design/homelander-emf-animations.md}:
 * TAKEOFF plays once fully then crossfades into the loop state; BOOST is only
 * ever reached while moving fast <em>forward</em> (backward flight at any
 * speed stays in HOVER — the BACKWARD RULE); LANDING / no flight state drops
 * to IDLE, fading every clip out.
 */
public final class HomelanderFlightMachine {
	public enum ServerPhase {
		NONE, TAKEOFF, AIRBORNE, BOOST, LANDING
	}

	public enum Phase {
		IDLE, TAKEOFF, HOVER, BOOST
	}

	/** Forward speed (blocks/tick along look) required to show BOOST. */
	public static final double BOOST_MIN_FWD_SPEED = 0.9;

	public record Input(ServerPhase serverPhase, double forwardSpeed) {
	}

	private Phase phase = Phase.IDLE;

	public Phase phase() {
		return phase;
	}

	/**
	 * @param input      server phase + signed forward speed (positive = moving
	 *                   toward look direction)
	 * @param takeoffDone whether the takeoff clip reached its last frame
	 */
	public Phase update(Input input, boolean takeoffDone) {
		switch (input.serverPhase()) {
			case NONE, LANDING -> phase = Phase.IDLE;
			case TAKEOFF -> {
				if (phase != Phase.TAKEOFF) {
					phase = Phase.TAKEOFF;
				}
			}
			case AIRBORNE, BOOST -> {
				if (phase == Phase.TAKEOFF) {
					// The takeoff clip must play out fully before loops resume.
					phase = takeoffDone ? loopPhase(input) : Phase.TAKEOFF;
				} else {
					phase = loopPhase(input);
				}
			}
		}
		return phase;
	}

	private static Phase loopPhase(Input input) {
		// BACKWARD RULE: negative forward speed never selects BOOST.
		if (input.serverPhase() == ServerPhase.BOOST
				&& input.forwardSpeed() >= BOOST_MIN_FWD_SPEED) {
			return Phase.BOOST;
		}
		return Phase.HOVER;
	}

	public void reset() {
		phase = Phase.IDLE;
	}
}
