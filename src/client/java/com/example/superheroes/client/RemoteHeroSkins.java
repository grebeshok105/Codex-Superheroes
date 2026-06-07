package com.example.superheroes.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RemoteHeroSkins {
	private static final Map<UUID, ResourceLocation> SKINS = new HashMap<>();

	private RemoteHeroSkins() {
	}

	public static void put(UUID playerId, @Nullable ResourceLocation heroId) {
		if (heroId == null) {
			SKINS.remove(playerId);
		} else {
			SKINS.put(playerId, heroId);
		}
	}

	@Nullable
	public static ResourceLocation get(UUID playerId) {
		return SKINS.get(playerId);
	}

	public static void clear() {
		SKINS.clear();
	}
}
