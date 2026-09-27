package io.github.grebeshok105.codex.core.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Consulted by {@code LivingEntityEffectMixin} at {@code LivingEntity.addEffect(instance, source)}
 * HEAD. Hero modules register a gate from {@code register(HeroModuleContext)} so the mixin — and
 * shared code in general — never names a concrete module class. First gate to deny wins; no gates
 * means every effect applies.
 */
public final class MobEffectGates {
	@FunctionalInterface
	public interface Gate {
		/** @return {@code false} to veto applying this effect to the target player. */
		boolean allow(ServerPlayer target, MobEffectInstance instance);
	}

	private static final List<Gate> GATES = new CopyOnWriteArrayList<>();

	private MobEffectGates() {
	}

	public static void register(Gate gate) {
		GATES.add(gate);
	}

	public static boolean allow(ServerPlayer target, MobEffectInstance instance) {
		for (Gate gate : GATES) {
			if (!gate.allow(target, instance)) return false;
		}
		return true;
	}
}
