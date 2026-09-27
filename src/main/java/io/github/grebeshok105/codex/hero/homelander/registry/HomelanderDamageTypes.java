package io.github.grebeshok105.codex.hero.homelander.registry;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.damage.DamageSources;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.damage.ModDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Homelander's damage types. Keys stay byte-identical to the row {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class HomelanderDamageTypes {
	public static final ResourceKey<DamageType> EYE_LASER = key("eye_laser");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("eye_laser", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_ARMOR, DamageTypeTags.BYPASSES_COOLDOWN, ModDamageTypes.BEAM)
	);

	private HomelanderDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource eyeLaser(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, EYE_LASER, attacker);
	}
}
