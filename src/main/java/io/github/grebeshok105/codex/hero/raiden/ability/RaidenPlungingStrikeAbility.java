package io.github.grebeshok105.codex.hero.raiden.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.hero.raiden.runtime.HeavensStrikeController;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RaidenPlungingStrikeAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("raiden_plunging_strike");
	private static final float COST = 280f;
	private static final int COOLDOWN_TICKS = 18 * 20;

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
		return COST;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (HeavensStrikeController.isCharging(player)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!HeavensStrikeController.start(player, HeavensStrikeController.Variant.RAIDEN)) {
			return false;
		}
		player.displayClientMessage(
				Component.translatable("ability.superheroes.raiden_plunging_strike.charging", "4.0"), true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
