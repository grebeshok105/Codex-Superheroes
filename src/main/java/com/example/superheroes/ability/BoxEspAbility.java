package com.example.superheroes.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class BoxEspAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.BOX_ESP;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 1.2f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return true;
	}
}
