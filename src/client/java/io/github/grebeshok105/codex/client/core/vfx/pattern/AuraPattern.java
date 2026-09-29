package io.github.grebeshok105.codex.client.core.vfx.pattern;

import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffectFactory;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Entity-following ambient field — the {@code fields} capability from the
 * design spec §4: a periodic Quasar emitter anchored to the entity plus an
 * optional dynamic light that tracks it. Runs for {@code durationTicks}
 * ({@code <= 0} = until the entity is removed or the effect is cancelled).
 * The light is removed on both the natural end and budget eviction.
 *
 * <p>Tuning keys: {@code emitIntervalTicks} (default 5),
 * {@code durationTicks} (default −1), {@code lightColor} (absent = no
 * light — presence, not sign, decides: {@code #AARRGGBB} with alpha ≥ 0x80
 * parses to a negative int), {@code lightRadius} (default 8),
 * {@code lightBrightness} (default 1), {@code height} (fraction of entity
 * height, default 0.5).
 */
public final class AuraPattern implements VfxEffect {
	private final @Nullable Entity entity;
	private final Vec3 fallbackPos;
	private final ResourceLocation emitter;
	private final int emitIntervalTicks;
	private final int durationTicks;
	private final boolean hasLight;
	private final int lightRgb;
	private final float lightRadius;
	private final float lightBrightness;
	private final float heightFraction;

	private int age;
	private @Nullable LightHandle light;
	private boolean finished;

	public AuraPattern(@Nullable Entity entity, Vec3 fallbackPos, ResourceLocation emitter,
			VfxParams params) {
		this.entity = entity;
		this.fallbackPos = fallbackPos;
		this.emitter = emitter;
		this.emitIntervalTicks = Math.max(1, (int) params.number("emitIntervalTicks", 5f));
		this.durationTicks = (int) params.number("durationTicks", -1f);
		this.hasLight = params.colors().containsKey("lightColor");
		this.lightRgb = params.color("lightColor", 0xFFFFFFFF);
		this.lightRadius = params.number("lightRadius", 8f);
		this.lightBrightness = params.number("lightBrightness", 1f);
		this.heightFraction = params.number("height", 0.5f);
	}

	/** Factory for {@code VfxRuntime.registerEffect}; follows the spawn's source entity. */
	public static VfxEffectFactory factory(ResourceLocation emitter) {
		return spawn -> new AuraPattern(spawn.source(), spawn.origin(), emitter, spawn.params());
	}

	@Override
	public void tick() {
		if (finished) {
			return;
		}
		age++;
		Vec3 center = center();
		if (center == null) {
			finish();
			return;
		}
		if (age % emitIntervalTicks == 0) {
			VfxBackends.current().emit(emitter, center);
		}
		if (hasLight) {
			if (light == null) {
				light = VfxBackends.current().light(center, lightRgb, lightRadius, lightBrightness);
			} else {
				light.move(center);
			}
		}
		if (durationTicks > 0 && age >= durationTicks) {
			finish();
		}
	}

	@Override
	public void render(VfxRenderContext ctx) {
		// Emitters and lights render themselves through the backend.
	}

	@Override
	public boolean done() {
		return finished;
	}

	@Override
	public void cancel() {
		finish();
	}

	private void finish() {
		finished = true;
		if (light != null) {
			light.remove();
			light = null;
		}
	}

	private @Nullable Vec3 center() {
		Entity tracked = entity;
		if (tracked != null) {
			if (tracked.isRemoved()) {
				return null;
			}
			return tracked.position().add(0, tracked.getBbHeight() * heightFraction, 0);
		}
		return fallbackPos;
	}
}
