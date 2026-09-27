package io.github.grebeshok105.codex.bootstrap;

import io.github.grebeshok105.codex.core.module.AbilitySink;
import io.github.grebeshok105.codex.mechanic.ability.FlightAbility;
import io.github.grebeshok105.codex.mechanic.ability.ViltrumiteChargeAbility;
import io.github.grebeshok105.codex.mechanic.ability.ViltrumiteRecoveryAbility;

/** Composition root for abilities shared by several heroes, so no single hero module may own them. */
public final class SharedAbilities {
	private SharedAbilities() {
	}

	public static void register(AbilitySink abilities) {
		abilities.register(new FlightAbility());
		abilities.register(new ViltrumiteRecoveryAbility());
		abilities.register(new ViltrumiteChargeAbility());
	}
}
