package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.render.BeamLook;
import io.github.grebeshok105.codex.client.core.vfx.VfxChannelEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.pattern.BeamPattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.PhaseTimeline;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

/**
 * The greed-magnet golden thread ({@code superheroes:regulus/greed_magnet}):
 * a thin gold beam from the caster's chest to the victim's chest plus spark
 * motes along the line and a light distortion shimmer on the victim. The
 * server retargets the victim position every {@code CHANNEL_UPDATE_INTERVAL_TICKS};
 * the client lerps between updates.
 */
public final class GreedMagnetChannel implements VfxChannelEffect {
	private static final ResourceLocation SPARK_EMITTER = ModId.of("regulus_greed_thread");
	private static final float RETARGET_TICKS = 2f;
	private static final int SPARK_INTERVAL = 4;
	private static final double CHEST = 0.6;

	private final Entity source;
	private final VfxParams params;
	private final PhaseTimeline timeline;
	private final BeamLook look;

	private Vec3 previousEnd;
	private Vec3 serverEnd;
	private int retargetAge;
	private int age;
	private int releasedAtAge = -1;

	public GreedMagnetChannel(Entity source, Vec3 target, VfxParams params) {
		this.source = source;
		this.params = params;
		this.serverEnd = target;
		this.previousEnd = target;
		this.timeline = new PhaseTimeline(
				(int) params.number("chargeTicks", 2f),
				(int) params.number("releaseTicks", 6f));
		this.look = new BeamLook(
				params.number("coreWidth", 0.02f),
				params.number("glowWidth", 0.07f),
				params.color("coreColor", 0xFFFFE870),
				params.color("glowColor", 0x60C89000),
				params.number("noise", 0.4f));
	}

	@Override
	public void tick() {
		age++;
		retargetAge++;
		if (timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE) {
			return;
		}
		float intensity = timeline.intensity(age, releasedAtAge, 0f);
		if (intensity > 0.1f && age % SPARK_INTERVAL == 0) {
			Vec3 from = chestOf(source);
			Vec3 end = endAt(0f);
			VfxBackends.current().emit(SPARK_EMITTER, from.lerp(end, 0.5));
			VfxBackends.current().distortion(end, params.number("victimDistortionRadius", 0.5f),
					params.number("victimDistortionStrength", 0.25f));
		}
	}

	@Override
	public void retarget(Vec3 target) {
		previousEnd = endAt(0f);
		serverEnd = target;
		retargetAge = 0;
	}

	@Override
	public void release() {
		if (releasedAtAge < 0) {
			releasedAtAge = age;
		}
	}

	@Override
	public void render(VfxRenderContext ctx) {
		float intensity = timeline.intensity(age, releasedAtAge, ctx.partialTick());
		if (intensity <= 0.02f) {
			return;
		}
		BeamPattern.draw(ctx, chestOf(source), endAt(ctx.partialTick()), look, intensity);
	}

	@Override
	public boolean done() {
		return timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE;
	}

	private Vec3 endAt(float partial) {
		float t = Math.min(1f, (retargetAge + partial) / RETARGET_TICKS);
		return previousEnd.lerp(serverEnd, t);
	}

	private static Vec3 chestOf(Entity entity) {
		return entity.getPosition(1f).add(0, entity.getBbHeight() * CHEST, 0);
	}
}
