package io.github.grebeshok105.codex.mixin.hero.regulus;

import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

/**
 * Bridge from {@link LivingEntityFallDamageMixin} (a mixin package, which must not
 * reference {@code hero.regulus.*} internals) back into the Regulus madness controller.
 * Wired once at bootstrap by {@code RegulusModule#register}; {@code false} until then.
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
