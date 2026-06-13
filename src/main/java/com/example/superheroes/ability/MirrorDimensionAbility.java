package com.example.superheroes.ability;

import com.example.superheroes.effect.MirrorDimensionController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pandora — «Дом тщеславия» (House of Vanity). Toggle: opens a 50-block zone
 * centred on Pandora and drags EVERY nearby player inside, where each sees the
 * Acid Shaders "round world" spherical-inversion warp on their own client and
 * is trapped within the zone until the toggle ends. Pure sensory/spatial trap:
 * no damage, the crushing/killing lives in separate abilities.
 */
public final class MirrorDimensionAbility implements Ability {
	/** Acid Shaders option MODE: 4 = "Uptown" inversion (world curls up around the player). */
	public static final int ACID_MODE = 4;
	/** Acid Shaders option J: sphere scale, smaller = tighter loop. */
	public static final int ACID_SCALE = 16;

	private static final int COOLDOWN_TICKS = 10 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.MIRROR_DIMENSION;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 60f;
	}

	@Override
	public float costPerTick() {
		return 0.5f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		// «Дом тщеславия»: opens a zone centred on Pandora and drags in EVERY
		// player within 50 blocks (the controller handles who can render it).
		return MirrorDimensionController.start(player, ACID_MODE, ACID_SCALE);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		MirrorDimensionController.stop(player, true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
	}
}
