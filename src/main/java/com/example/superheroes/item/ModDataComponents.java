package com.example.superheroes.item;

import com.example.superheroes.ModId;
import com.example.superheroes.item.bound.BoundWeaponToken;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModDataComponents {
	public static final DataComponentType<BoundWeaponToken> BOUND_WEAPON = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			ModId.of("bound_weapon"),
			DataComponentType.<BoundWeaponToken>builder()
					.persistent(BoundWeaponToken.CODEC)
					.networkSynchronized(BoundWeaponToken.STREAM_CODEC)
					.build());

	private ModDataComponents() {
	}

	public static void init() {
	}
}
