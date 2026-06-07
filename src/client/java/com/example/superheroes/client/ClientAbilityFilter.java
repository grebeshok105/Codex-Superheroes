package com.example.superheroes.client;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.infinity.InfinityStoneType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class ClientAbilityFilter {
	private ClientAbilityFilter() {
	}

	public static List<ResourceLocation> visibleFor(List<ResourceLocation> base, ResourceLocation heroId) {
		boolean isDoomsday = ModId.of("doomsday").equals(heroId);
		boolean isThanos = ThanosHero.ID.equals(heroId);
		int doomsdayTier = isDoomsday ? ClientDoomsdayState.tier() : 0;
		ArrayList<ResourceLocation> out = new ArrayList<>(base.size());
		for (ResourceLocation id : base) {
			if (!ClientMadnessState.isMadness() && AbilityIds.COUNTER_STRIKE.equals(id)) continue;
			if (isDoomsday && !isDoomsdayUnlocked(id, doomsdayTier)) continue;
			if (isThanos && !isThanosUnlocked(id)) continue;
			out.add(id);
		}
		return out;
	}

	public static List<ResourceLocation> visible() {
		return visibleFor(ClientHeroState.abilities(), ClientHeroState.heroId());
	}

	private static boolean isThanosUnlocked(ResourceLocation id) {
		if (ThanosHero.isSnapAbility(id)) return ClientThanosState.hasAllStones();
		InfinityStoneType req = ThanosHero.getRequiredStoneFor(id);
		if (req == null) return true;
		return ClientThanosState.hasStone(req);
	}

	private static boolean isDoomsdayUnlocked(ResourceLocation id, int tier) {
		if (AbilityIds.DOOMSDAY_SMASH.equals(id)) return tier >= 2;
		if (AbilityIds.DOOMSDAY_ROAR.equals(id)) return tier >= 3;
		if (AbilityIds.DOOMSDAY_BONE_SPIKE.equals(id)) return tier >= 4;
		if (AbilityIds.DOOMSDAY_CHARGE_TACKLE.equals(id)) return tier >= 5;
		if (AbilityIds.DOOMSDAY_BERSERK.equals(id)) return tier >= 6;
		if (AbilityIds.DOOMSDAY_DOOM_GRIP.equals(id)) return tier >= 7;
		return true;
	}
}
