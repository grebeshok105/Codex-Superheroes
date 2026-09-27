package io.github.grebeshok105.codex.mechanic.flight;

import io.github.grebeshok105.codex.mechanic.ability.SharedAbilityIds;
import io.github.grebeshok105.codex.core.transform.HeroData;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightAbilityStateTest {
	@Test
	void supersonicHasPriorityButIronManRemainsWhenSupersonicIsRemoved() {
		HeroData data = new HeroData(Optional.empty(), 100f, 0f, Map.of(),
				Set.of(SharedAbilityIds.IRON_MAN_FLIGHT, SharedAbilityIds.SUPERSONIC));

		assertEquals(FlightMode.SUPERSONIC, FlightAbilityState.activeMode(data));
		assertEquals(FlightMode.IRON_MAN, FlightAbilityState.activeModeExcept(data, SharedAbilityIds.SUPERSONIC));
	}
}
