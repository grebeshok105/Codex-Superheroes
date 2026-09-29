package io.github.grebeshok105.codex.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClientFlightState} presentation-ownership marks: the pose tracker
 * marks players whose visuals it owns so legacy visual managers (the vanilla
 * flight trail) stand down; session reset clears the marks.
 */
class ClientFlightStateTest {

	@BeforeEach
	void freshState() {
		ClientFlightState.clearAll();
	}

	@Test
	void markAndQuery() {
		assertFalse(ClientFlightState.isPresentationOwned(7));
		ClientFlightState.markPresentationOwned(7);
		assertTrue(ClientFlightState.isPresentationOwned(7));
		assertFalse(ClientFlightState.isPresentationOwned(8));
	}

	@Test
	void unmarkReleases() {
		ClientFlightState.markPresentationOwned(7);
		ClientFlightState.unmarkPresentationOwned(7);
		assertFalse(ClientFlightState.isPresentationOwned(7));
	}

	@Test
	void clearAllDropsMarks() {
		ClientFlightState.markPresentationOwned(7);
		ClientFlightState.clearAll();
		assertFalse(ClientFlightState.isPresentationOwned(7));
	}
}
