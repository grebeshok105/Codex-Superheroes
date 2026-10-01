package io.github.grebeshok105.codex.client.hero.regulus.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.vfx.VfxChannelEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.pattern.PhaseTimeline;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientHeartsState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The hearts-in-mobs pulse ({@code superheroes:regulus/heart_pulse}): a weak
 * golden shimmer breathes above every mob carrying the player's hearts.
 * Owner-only in practice — {@link ClientHeartsState} is only fed by the
 * owner's own {@code HeartsSyncS2CPayload}; the channel simply opens on the
 * empty→non-empty transition and closes when the last heart drops.
 */
public final class HeartPulseChannel implements VfxChannelEffect {
	private static final ResourceLocation PULSE_EMITTER = ModId.of("regulus_heart_pulse");
	private static final int EMIT_INTERVAL = 9;

	private final Entity source;
	private final VfxParams params;
	private final PhaseTimeline timeline;

	private int age;
	private int releasedAtAge = -1;

	public HeartPulseChannel(Entity source, Vec3 target, VfxParams params) {
		this.source = source;
		this.params = params;
		this.timeline = new PhaseTimeline(
				(int) params.number("chargeTicks", 8f),
				(int) params.number("releaseTicks", 6f));
	}

	@Override
	public void tick() {
		age++;
		if (timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE) {
			return;
		}
		if (age % EMIT_INTERVAL != 0) {
			return;
		}
		float intensity = timeline.intensity(age, releasedAtAge, 0f);
		if (intensity <= 0.05f || source != Minecraft.getInstance().player) {
			return;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		for (int entityId : ClientHeartsState.heartEntityIds()) {
			Entity bearer = level.getEntity(entityId);
			if (bearer == null) {
				continue;
			}
			Vec3 head = bearer.position().add(0, bearer.getBbHeight() + 0.3, 0);
			VfxBackends.current().emit(PULSE_EMITTER, head);
		}
	}

	@Override
	public void retarget(Vec3 target) {
		// Keepalive only — heart positions come from the synced state.
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
