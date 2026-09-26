package com.example.superheroes.bootstrap;

import com.example.superheroes.ability.FlightAbility;
import com.example.superheroes.ability.ViltrumiteRecoveryAbility;
import com.example.superheroes.core.module.AbilitySink;

/** Composition root for abilities shared by several heroes, so no single hero module may own them. */
public final class SharedAbilities {
	private SharedAbilities() {
	}

	public static void register(AbilitySink abilities) {
		abilities.register(new FlightAbility());
		abilities.register(new ViltrumiteRecoveryAbility());
	}
}
