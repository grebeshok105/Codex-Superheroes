package io.github.grebeshok105.codex.client.core.emf;

import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Hero-agnostic gate hiding a living entity's third-person held items (the
 * {@code ItemInHandLayer} feature layer). A hero registers a predicate when
 * an authored EMF prop replaces the vanilla held item — Homelander's milk
 * bottle while its milk weight is up. First-person hands render through
 * {@code ItemInHandRenderer}, not this layer, so they are untouched here.
 * Empty when EMF is absent: hero registration only happens under
 * {@link EmfBridge#isAvailable()}.
 */
public final class EmfHeldItemSuppression {

	private static final CopyOnWriteArrayList<Predicate<LivingEntity>> PREDICATES =
			new CopyOnWriteArrayList<>();

	private EmfHeldItemSuppression() {
	}

	public static void register(Predicate<LivingEntity> predicate) {
		PREDICATES.add(predicate);
	}

	/** True when a registered EMF presentation replaces this entity's held items. */
	public static boolean hides(LivingEntity entity) {
		for (Predicate<LivingEntity> predicate : PREDICATES) {
			if (predicate.test(entity)) {
				return true;
			}
		}
		return false;
	}
}
