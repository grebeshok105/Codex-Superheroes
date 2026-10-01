package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.vfx.VfxChannelEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.pattern.PhaseTimeline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The Lion's Heart void dome ({@code superheroes:regulus/lion_heart_dome}):
 * while protection is up, a light distortion halo rides the player and cold
 * white motes shimmer around them — the "frozen projectiles tinted white"
 * materialization of the void heart. The server keeps the channel alive
 * (target = the player's position) so it follows the owner.
 */
public final class LionHeartDomeChannel implements VfxChannelEffect {
	private static final ResourceLocation SHIMMER_EMITTER = ModId.of("regulus_lion_heart_shimmer");
	private static final int DISTORTION_INTERVAL = 2;
	private static final int SHIMMER_INTERVAL = 5;

	private final Entity source;
	private final VfxParams params;
	private final PhaseTimeline timeline;

	private int age;
	private int releasedAtAge = -1;

	public LionHeartDomeChannel(Entity source, Vec3 target, VfxParams params) {
		this.source = source;
		this.params = params;
		this.timeline = new PhaseTimeline(
				(int) params.number("chargeTicks", 6f),
				(int) params.number("releaseTicks", 10f));
	}

	@Override
	public void tick() {
		age++;
		if (timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE) {
			return;
		}
		float intensity = timeline.intensity(age, releasedAtAge, 0f);
		if (intensity <= 0.05f) {
			return;
		}
		Vec3 center = source.position().add(0, source.getBbHeight() * 0.5, 0);
		if (age % DISTORTION_INTERVAL == 0) {
			VfxBackends.current().distortion(center,
					params.number("domeRadius", 2.6f),
					params.number("distortionStrength", 0.3f) * intensity);
		}
		if (age % SHIMMER_INTERVAL == 0) {
			VfxBackends.current().emit(SHIMMER_EMITTER, center);
		}
	}

	@Override
	public void retarget(Vec3 target) {
		// The dome rides the entity; retarget updates are keepalives.
	}

	@Override
	public void release() {
		if (releasedAtAge < 0) {
			releasedAtAge = age;
		}
	}

	@Override
	public void render(VfxRenderContext ctx) {
	}

	@Override
	public boolean done() {
		return timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE;
	}
}
