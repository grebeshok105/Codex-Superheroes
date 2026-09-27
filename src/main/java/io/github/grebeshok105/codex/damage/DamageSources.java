package io.github.grebeshok105.codex.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/** Builds {@link DamageSource} instances for ability damage types (same shape as the old {@code ModDamageTypes.source}). */
public final class DamageSources {
	private DamageSources() {
	}

	public static DamageSource of(ServerLevel level, ResourceKey<DamageType> key, Entity attacker) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(key), attacker, attacker);
	}
}
