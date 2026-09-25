package com.example.superheroes.falbiks;

import com.example.superheroes.SuperheroesMod;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Fail-soft access to the falbiks {@code HeroComponent#player} field.
 *
 * <p>{@code HeroComponentStripMixin} is a {@code @Pseudo} mixin into another
 * mod's class: a {@code @Shadow} field is applied unconditionally at mixin
 * apply time, so an upstream field rename would crash the game — {@code
 * require = 0} on the injects does not cover it. Reflection resolved once here
 * degrades the whole hook to a no-op instead (one warning logged).
 */
public final class HeroComponentAccess {
	private static volatile Field playerField;
	private static volatile boolean resolved;

	private HeroComponentAccess() {
	}

	@Nullable
	public static Player playerOf(Object component) {
		if (!resolved) {
			resolve(component.getClass());
		}
		Field field = playerField;
		if (field == null) {
			return null;
		}
		try {
			return field.get(component) instanceof Player player ? player : null;
		} catch (IllegalAccessException | RuntimeException e) {
			return null;
		}
	}

	private static synchronized void resolve(Class<?> componentClass) {
		if (resolved) {
			return;
		}
		resolved = true;
		try {
			Field field = null;
			for (Class<?> c = componentClass; c != null && field == null; c = c.getSuperclass()) {
				try {
					field = c.getDeclaredField("player");
				} catch (NoSuchFieldException ignored) {
				}
			}
			if (field == null || !Player.class.isAssignableFrom(field.getType())) {
				throw new NoSuchFieldException("player");
			}
			field.setAccessible(true);
			playerField = field;
		} catch (NoSuchFieldException | RuntimeException e) {
			playerField = null;
			SuperheroesMod.LOGGER.warn(
					"falbiks HeroComponent no longer exposes a 'player' field — vanity power-strip disabled ({})",
					e.toString());
		}
	}
}
