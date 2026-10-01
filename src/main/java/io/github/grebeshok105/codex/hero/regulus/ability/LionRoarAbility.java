package io.github.grebeshok105.codex.hero.regulus.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.regulus.runtime.DebrisKickController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * The debris kick — authored-cast shotgun on the persisted {@code lion_roar} slot.
 * Press starts a 14-tick windup (the kick clip, Slowness I for the cast window);
 * the fire tick runs {@link DebrisKickController#fire}, charges the activation
 * cost, and arms the cooldown. Damage taken before the fire tick cancels the cast
 * for free; a manual cancel does the same. The ability id and its bound key never
 * change — persisted loadouts stay valid across the rework.
 */
public final class LionRoarAbility implements Ability {
	private static final int FIRE_TICKS = 14;
	private static final int CAST_UNTIL_TICKS = 44;
	private static final float ACTIVATE_COST = 250f;
	private static final int COOLDOWN_TICKS = 400;

	public static final ResourceLocation ID = ModId.of("lion_roar");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	/** The router sees a free press — the cast machine charges on the authored fire tick. */
	@Override
	public float costOnActivate() {
		return 0f;
	}

	/** The HUD still advertises the real price even though the press itself is free. */
	@Override
	public float displayCostOnActivate() {
		return ACTIVATE_COST;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!RegulusCastState.startCast(player, new RegulusCastState.Spec(
				ID, FIRE_TICKS, CAST_UNTIL_TICKS, ACTIVATE_COST, COOLDOWN_TICKS, true,
				() -> DebrisKickController.fire(player),
				() -> player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN)))) {
			return false;
		}
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				CAST_UNTIL_TICKS, 0, true, false, true));
		Vec3 eye = player.getEyePosition();
		VfxFx.event(player, RegulusVfxIds.ANIM_DEBRIS_KICK, eye,
				eye.add(player.getViewVector(1f).scale(DebrisKickController.RANGE)), 1f);
		return true;
	}
}
