package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Hero-registered third-person camera recenter. A flight presentation that
 * tilts the rendered body off the vanilla eye point registers a provider
 * {@code (entity, tickDelta) -> offset blocks}; {@code CameraMixin} asks
 * {@link #offsetFor} and slides the camera by the returned vector. Shared
 * core holds the registry, hero modules own the math — same shape as
 * {@link io.github.grebeshok105.codex.client.core.render.PlayerModelSuppressions}.
 */
public final class FlightCameraFocus {
	private static final List<BiFunction<LivingEntity, Float, Vec3>> PROVIDERS = new ArrayList<>();

	private FlightCameraFocus() {
	}

	/** Called from client-module bootstrap only. */
	public static void register(BiFunction<LivingEntity, Float, Vec3> provider) {
		PROVIDERS.add(provider);
	}

	/** First non-null camera offset, or {@code null} when no provider recentered. */
	public static @Nullable Vec3 offsetFor(LivingEntity entity, float tickDelta) {
		for (BiFunction<LivingEntity, Float, Vec3> provider : PROVIDERS) {
			Vec3 offset = provider.apply(entity, tickDelta);
			if (offset != null) {
				return offset;
			}
		}
		return null;
	}
}
