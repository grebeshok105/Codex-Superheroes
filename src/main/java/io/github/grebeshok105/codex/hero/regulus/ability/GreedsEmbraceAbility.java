package io.github.grebeshok105.codex.hero.regulus.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.regulus.runtime.GreedStasisController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Greed's Embrace → stasis field. A 2.2s cast: the acquire phase (0–13t) locks
 * the aimed anchor, the 18t fire tick opens a stasis dome there (charged 400,
 * cooldown 700 — both land on fire, not on activate). Damage during the windup
 * interrupts the cast for free.
 */
public final class GreedsEmbraceAbility implements Ability {
	static final int FIRE_TICK = 18;
	static final int CAST_UNTIL_TICK = 44;
	static final float ACTIVATE_COST = 400f;
	static final int COOLDOWN_TICKS = 700;

	public static final ResourceLocation ID = ModId.of("greeds_embrace");

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
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!RegulusCastState.startCast(player, new RegulusCastState.Spec(
				ID, FIRE_TICK, CAST_UNTIL_TICK, ACTIVATE_COST, COOLDOWN_TICKS, true,
				() -> GreedStasisController.tryOpen(player),
				() -> GreedStasisController.cancelPending(player)))) {
			return false;
		}
		GreedStasisController.beginCast(player);
		Vec3 origin = player.getEyePosition();
		VfxFx.event(player, RegulusVfxIds.ANIM_GREEDS_EMBRACE_CAST, origin, origin, 1f);
		return true;
	}
}
