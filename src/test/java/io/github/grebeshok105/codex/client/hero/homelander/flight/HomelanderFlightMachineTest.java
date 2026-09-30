package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightMachine.Input;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightMachine.Phase;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightMachine.ServerPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomelanderFlightMachineTest {

	@Test
	void takeoffPlaysFullyBeforeLoops() {
		HomelanderFlightMachine m = new HomelanderFlightMachine();
		assertEquals(Phase.IDLE, m.update(new Input(ServerPhase.NONE, 0), false));
		assertEquals(Phase.TAKEOFF, m.update(new Input(ServerPhase.TAKEOFF, 0), false));
		// Server moves on early — the clip must still play out.
		assertEquals(Phase.TAKEOFF, m.update(new Input(ServerPhase.AIRBORNE, 0), false));
		assertEquals(Phase.HOVER, m.update(new Input(ServerPhase.AIRBORNE, 0), true));
	}

	@Test
	void boostRequiresFastForwardMotion() {
		HomelanderFlightMachine m = new HomelanderFlightMachine();
		m.update(new Input(ServerPhase.AIRBORNE, 0), false);
		assertEquals(Phase.BOOST,
				m.update(new Input(ServerPhase.BOOST, 2.0), false));
		assertEquals(Phase.HOVER,
				m.update(new Input(ServerPhase.AIRBORNE, 0.2), false));
	}

	@Test
	void backwardRuleNeverSelectsBoost() {
		HomelanderFlightMachine m = new HomelanderFlightMachine();
		m.update(new Input(ServerPhase.AIRBORNE, 0), false);
		// Fast backward travel: server may even say BOOST — stay HOVER.
		assertEquals(Phase.HOVER,
				m.update(new Input(ServerPhase.BOOST, -3.0), false));
		assertEquals(Phase.HOVER,
				m.update(new Input(ServerPhase.BOOST, 0.3), false));
	}

	@Test
	void landingAndNoneFadeToIdle() {
		HomelanderFlightMachine m = new HomelanderFlightMachine();
		m.update(new Input(ServerPhase.BOOST, 2.0), false);
		assertEquals(Phase.IDLE, m.update(new Input(ServerPhase.LANDING, 0), false));
		m.update(new Input(ServerPhase.AIRBORNE, 0), false);
		assertEquals(Phase.IDLE, m.update(new Input(ServerPhase.NONE, 0), false));
	}
}
