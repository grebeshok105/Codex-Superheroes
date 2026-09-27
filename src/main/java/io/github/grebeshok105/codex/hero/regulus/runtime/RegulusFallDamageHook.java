package io.github.grebeshok105.codex.hero.regulus.runtime;

import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

/**
 * Bridge from {@code mixin.hero.regulus.LivingEntityFallDamageMixin} into the Regulus
 * madness controller: mixin-package classes may not reference the module (and normal
 * code may not reference mixin-package classes), so this module-owned class holds the
 * predicate the mixin consults. Wired once at bootstrap by {@code RegulusModule#register};
 * {@code false} until then.
 */
public final class RegulusFallDamageHook {
	private static volatile Predicate<Entity> counterParticipant = e -> false;

	private RegulusFallDamageHook() {
	}

	public static void wire(Predicate<Entity> predicate) {
		counterParticipant = predicate;
	}

	/** {@code true} while {@code entity} is a participant of an active Regulus counter. */
	public static boolean isCounterParticipant(Entity entity) {
		return counterParticipant.test(entity);
	}
}
