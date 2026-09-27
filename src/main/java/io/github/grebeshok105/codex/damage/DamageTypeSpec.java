package io.github.grebeshok105.codex.damage;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

import java.util.Set;

/**
 * A hero-owned damage type declaration: the registry key, the exact {@link DamageType}
 * definition to bootstrap in datagen, and the tag keys the type belongs to. Returned by
 * {@code HeroModule.damageTypes()}; {@code HeroModules} enumerates the specs so datagen
 * and tag providers never name hero classes directly.
 */
public record DamageTypeSpec(ResourceKey<DamageType> key, DamageType type, Set<TagKey<DamageType>> tags) {
	public static DamageTypeSpec of(String path, DamageScaling scaling, float exhaustion, TagKey<DamageType>... tags) {
		return new DamageTypeSpec(
				ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(path)),
				new DamageType(path, scaling, exhaustion),
				Set.of(tags));
	}

	public static DamageTypeSpec of(String path, DamageScaling scaling, float exhaustion, DamageEffects effects,
			TagKey<DamageType>... tags) {
		return new DamageTypeSpec(
				ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(path)),
				new DamageType(path, scaling, exhaustion, effects),
				Set.of(tags));
	}
}
