package io.github.grebeshok105.codex.hero.rem.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.hero.rem.runtime.RemDemonismController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RemMaceCraterAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("rem_mace_crater");

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
		return 75f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return RemDemonismController.isActive(player)
				&& !RemDemonismController.isCraterWinding(player);
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return RemDemonismController.startCraterAttack(player);
	}
}
