package com.example.superheroes.client.fx;

import com.example.superheroes.ModId;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Veil/Quasar implementation of Scorpion's hellfire effects. Loaded only when the
 * "veil" mod is present (guarded by {@link ClientScorpionFx}); every spawn is wrapped
 * in try/catch because Quasar emitters are resource-pack driven and fault-tolerant.
 */
public final class VeilScorpionFx {
	private static final ResourceLocation PILLAR = ModId.of("scorpion_hellfire");
	private static final ResourceLocation HARPOON = ModId.of("scorpion_harpoon");
	private static final ResourceLocation TELEPORT = ModId.of("scorpion_teleport");
	private static final ResourceLocation BREATH = ModId.of("scorpion_hellbreath");

	private VeilScorpionFx() {
	}

	public static void harpoon(Vec3 from, Vec3 to) {
		int steps = 6;
		for (int i = 0; i <= steps; i++) {
			spawn(HARPOON, from.lerp(to, i / (double) steps));
		}
		spawn(PILLAR, to); // small flare where it skewers
	}

	public static void pillar(Vec3 at) {
		spawn(PILLAR, at);
	}

	public static void teleport(Vec3 at) {
		spawn(TELEPORT, at);
	}

	public static void breath(Vec3 mouth, Vec3 dir) {
		Vec3 d = dir.lengthSqr() < 1.0e-6 ? new Vec3(0, 0, 1) : dir.normalize();
		double[] dist = {1.5, 3.0, 4.5, 6.0};
		for (double t : dist) {
			spawn(BREATH, mouth.add(d.scale(t)));
		}
	}

	private static void spawn(ResourceLocation id, Vec3 pos) {
		try {
			ParticleSystemManager manager = VeilRenderSystem.renderer().getParticleManager();
			ParticleEmitter emitter = manager.createEmitter(id);
			if (emitter == null) {
				return;
			}
			emitter.setPosition(pos);
			manager.addParticleSystem(emitter);
		} catch (Throwable ignored) {
			// Resource pack / renderer not ready — fail silently, vanilla particles already cover it.
		}
	}
}
