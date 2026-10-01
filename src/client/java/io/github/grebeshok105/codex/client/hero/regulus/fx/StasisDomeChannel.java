package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.vfx.VfxChannelEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.pattern.PhaseTimeline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Greed's-Embrace stasis dome ({@code superheroes:regulus/greed_stasis}):
 * a glassy distortion bubble over the embrace anchor — the channel target
 * is the anchor position, sent at open and as a keepalive while the dome
 * holds. A faint cold light sits inside and a ground ring of pale motes
 * traces the dome's base.
 */
public final class StasisDomeChannel implements VfxChannelEffect {
	private static final ResourceLocation RING_EMITTER = ModId.of("regulus_stasis_ring");
	private static final int RING_INTERVAL = 7;
	private static final int DISTORTION_INTERVAL = 4;

	private final Entity source;
	private final VfxParams params;
	private final PhaseTimeline timeline;
	private final float radius;

	private Vec3 anchor;
	private int age;
	private int releasedAtAge = -1;
	private @Nullable LightHandle light;

	public StasisDomeChannel(Entity source, Vec3 target, VfxParams params) {
		this.source = source;
		this.params = params;
		this.anchor = target;
		this.radius = params.number("domeRadius", 8f);
		this.timeline = new PhaseTimeline(
				(int) params.number("chargeTicks", 10f),
				(int) params.number("releaseTicks", 8f));
	}

	@Override
	public void tick() {
		age++;
		if (timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE) {
			removeLight();
			return;
		}
		float intensity = timeline.intensity(age, releasedAtAge, 0f);
		if (age % DISTORTION_INTERVAL == 0 && intensity > 0.05f) {
			VfxBackends.current().distortion(anchor.add(0, radius * 0.4, 0),
					params.number("distortionRadius", radius * 0.55f),
					params.number("distortionStrength", 0.35f) * intensity);
		}
		if (age % RING_INTERVAL == 0 && intensity > 0.1f) {
			VfxBackends.current().emit(RING_EMITTER, anchor);
		}
		if (intensity > 0.1f) {
			Vec3 lightPos = anchor.add(0, radius * 0.5, 0);
			if (light == null) {
				light = VfxBackends.current().light(lightPos,
						params.color("lightColor", 0xA8D8E0) & 0xFFFFFF,
						params.number("lightRadius", radius * 0.8f),
						params.number("lightBrightness", 0.4f) * intensity);
			}
		}
	}

	@Override
	public void retarget(Vec3 target) {
		anchor = target;
		if (light != null) {
			light.move(target.add(0, radius * 0.5, 0));
		}
	}

	@Override
	public void release() {
		if (releasedAtAge < 0) {
			releasedAtAge = age;
			removeLight();
		}
	}

	@Override
	public void render(VfxRenderContext ctx) {
	}

	@Override
	public boolean done() {
		return timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE;
	}

	@Override
	public void cancel() {
		removeLight();
	}

	private void removeLight() {
		if (light != null) {
			light.remove();
			light = null;
		}
	}
}
