package io.github.grebeshok105.codex.hero.homelander.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.homelander.runtime.IronFistsController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.resource.EnergyLocks;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class IronFistsAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("iron_fists");
	public static final int DURATION_TICKS = 200;
	public static final float MELEE_DAMAGE = 5.0f;
	public static final double MELEE_KNOCKBACK = 2.5;

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
		if (!data.hasHero()) return false;
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) return false;
		return data.energy() >= hero.getEnergyMax() - 0.001f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		HeroDataStore.update(player, d -> d.withEnergy(0f));

		EnergyLocks.lockTicks(player, DURATION_TICKS);
		IronFistsController.markActivated(player);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		IronFistsController.tickActive(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		IronFistsController.markDeactivated(player);
	}
}
