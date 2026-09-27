package io.github.grebeshok105.codex.ability;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.effect.UnibeamController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class UnibeamAbility implements Ability {
	private static final float ACTIVATION_GATE = 100f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.UNIBEAM;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return ACTIVATION_GATE;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return UnibeamController.startCharge(player);
	}
}
