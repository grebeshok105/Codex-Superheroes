package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.FlightController;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.UraniumDefenseController;
import com.example.superheroes.flight.FlightMode;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.transform.HeroData;
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
		return 0.5f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
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
