package io.github.grebeshok105.codex.client.core.flight;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Third-person camera pivot offsets. A hero whose presentation moves the
 * body's visual center off the vanilla eye point (Homelander's feet-pivot
 * flight tilt) registers a provider from its client module; the camera mixin
 * adds the first non-null offset so the tilted body stays centered. Hero-free
 * seam for {@code client.mixin} — mirroring {@code PlayerModelSuppressions}:
 * mixins must never import {@code client.hero.*} classes.
 */
public final class FlightCameraOffsets {
	@FunctionalInterface
	public interface Provider {
		/** Blocks to slide the camera by, or null when this provider doesn't own the entity. */
		@Nullable Vec3 offset(LivingEntity entity, float tickDelta);
	}

	private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

	private FlightCameraOffsets() {
	}

	/** Called from client-module registration only. */
	public static void register(Provider provider) {
		PROVIDERS.add(provider);
	}

	/** The first provider's offset for {@code entity}, or null when nobody owns it. */
	public static @Nullable Vec3 of(LivingEntity entity, float tickDelta) {
		for (Provider provider : PROVIDERS) {
			Vec3 offset = provider.offset(entity, tickDelta);
			if (offset != null) {
				return offset;
			}
		}
		return null;
	}
}
