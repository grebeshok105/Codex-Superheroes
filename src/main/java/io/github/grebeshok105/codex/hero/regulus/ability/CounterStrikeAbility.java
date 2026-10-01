package io.github.grebeshok105.codex.hero.regulus.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class CounterStrikeAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("counter_strike");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 200f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	public static final int COOLDOWN_TICKS = 800;

	@Override
	public boolean canActivate(ServerPlayer player) {
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
		if (!state.madness()) {
			return false;
		}
		return findTarget(player) != null;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		RegulusMadnessState state = player.getAttachedOrCreate(RegulusMadnessState.ATTACHMENT);
		if (!state.madness()) {
			return false;
		}
		LivingEntity target = findTarget(player);
		if (target == null) {
			return false;
		}
		RegulusMadnessController.triggerCounter(player, target);
		AbilityCooldowns.setCooldownTicks(player, ID, COOLDOWN_TICKS);
		return true;
	}

	private static LivingEntity findTarget(ServerPlayer player) {
		// Only the recorded last damager inside the controller's search range — no
		// getLastHurtByMob or nearest-hostile fallback (see RegulusMadnessController).
		return RegulusMadnessController.findCounterTarget(player);
	}
}
