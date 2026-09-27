package io.github.grebeshok105.codex.mechanic.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.mechanic.flight.FlightController;
import io.github.grebeshok105.codex.mechanic.flight.FlightMode;
import io.github.grebeshok105.codex.mechanic.flight.FlightModifier;
import io.github.grebeshok105.codex.mechanic.flight.FlightProfiles;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FlightAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("flight");

	@Override
	public ResourceLocation getId() {
		return ID;
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
		FlightModifier modifier = FlightProfiles.modifierFor(data);
		if (modifier != null && modifier.denyActivation(player, data)) {
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
