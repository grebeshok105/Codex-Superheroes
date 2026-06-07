package com.example.superheroes;

import net.minecraft.resources.ResourceLocation;

public final class ModId {
	public static final String MOD_ID = "superheroes";

	private ModId() {
	}

	public static ResourceLocation of(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
