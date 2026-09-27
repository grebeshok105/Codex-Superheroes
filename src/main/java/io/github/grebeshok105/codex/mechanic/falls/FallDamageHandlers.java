package io.github.grebeshok105.codex.mechanic.falls;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Shared fall-damage seam consulted by {@code mixin.hero.*} fall-damage mixins (mixin
 * packages are shared space and may never reference {@code hero.*} internals — §6.1.1).
 * Modules register predicates instead: an <b>immunity suppressor</b> declares that the
 * entity takes fall damage even when hero-level immunity would cancel it. Regulus uses
 * this for counter participants; BF9's per-player fall immunity plugs in here as the
 * opposite direction (an exemption side) without touching the mixin.
 */
public final class FallDamageHandlers {
	private static final List<Predicate<LivingEntity>> IMMUNITY_SUPPRESSORS = new CopyOnWriteArrayList<>();

	private FallDamageHandlers() {
	}

	/** {@code predicate.test(entity) == true} marks fall damage as still applying despite
	 *  hero-level immunity ({@code SuperJumpController} / {@code Hero.cancelsFallDamage}). */
	public static void registerImmunitySuppressor(Predicate<LivingEntity> predicate) {
		IMMUNITY_SUPPRESSORS.add(predicate);
	}

	/** {@code true} while any registered suppressor claims {@code entity}. */
	public static boolean isFallImmunitySuppressed(LivingEntity entity) {
		for (Predicate<LivingEntity> suppressor : IMMUNITY_SUPPRESSORS) {
			if (suppressor.test(entity)) {
				return true;
			}
		}
		return false;
	}
}
