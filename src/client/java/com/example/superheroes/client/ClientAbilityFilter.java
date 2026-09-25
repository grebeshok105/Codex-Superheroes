package com.example.superheroes.client;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.infinity.InfinityStoneType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class ClientAbilityFilter {
	private ClientAbilityFilter() {
	}

	public static List<ResourceLocation> visibleFor(List<ResourceLocation> base, ResourceLocation heroId) {
		// Vanity-stripped victims (#1): their abilities are fully REMOVED from every HUD —
		// the ability bar, the radial menu and the tooltip list all go empty, so the round
		// ability slots simply vanish, exactly like in the reference mod.
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && com.example.superheroes.effect.ModEffects.isVanityStripped(mc.player)) {
			return new ArrayList<>();
		}
		boolean isDoomsday = ModId.of("doomsday").equals(heroId);
		boolean isThanos = ThanosHero.ID.equals(heroId);
		boolean isRem = RemHero.ID.equals(heroId);
		boolean isPandora = PandoraHero.ID.equals(heroId);
		int doomsdayTier = isDoomsday ? ClientDoomsdayState.tier() : 0;
		boolean remDemonism = isRemDemonismActive();
		boolean houseOpen = isPandora && ClientPandoraHouseState.isOpen();
		ArrayList<ResourceLocation> out = new ArrayList<>(base.size());
		for (ResourceLocation id : base) {
			if (!ClientMadnessState.isMadness() && AbilityIds.COUNTER_STRIKE.equals(id)) continue;
			if (isDoomsday && !DoomsdayHero.isUnlockedAtTier(doomsdayTier, id)) continue;
			if (isThanos && !isThanosUnlocked(id)) continue;
			if (isRem && !RemHero.isVisibleIn(id, remDemonism)) continue;
			// Pandora: only the House entry shows until she is inside her House of Vanity (#2).
			if (isPandora && !houseOpen && PandoraHero.isDimensionOnly(id)) continue;
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

	private static boolean isRemDemonismActive() {
		if (Minecraft.getInstance().player == null) {
			return false;
		}
		return ClientRemDemonismState.isActive(Minecraft.getInstance().player.getUUID());
	}

}
