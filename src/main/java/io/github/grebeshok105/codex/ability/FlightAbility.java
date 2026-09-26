package io.github.grebeshok105.codex.ability;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.effect.FlightController;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.UraniumDefenseController;
import io.github.grebeshok105.codex.flight.FlightMode;
import io.github.grebeshok105.codex.hero.HomelanderHero;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FlightAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.FLIGHT;
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
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		boolean isHomelander = data.hasHero() && HomelanderHero.ID.equals(data.heroId());
		if (isHomelander && !ModEffects.isMadness(player)
				&& UraniumDefenseController.isUnderUraniumThreat(player)
				&& FlightController.isOnCooldown(player)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		return FlightController.start(player, FlightMode.NORMAL);
	}

	@Override
	public void onTickActive(ServerPlayer player) {
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		FlightController.stop(player, getId());
	}
}
