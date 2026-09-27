package io.github.grebeshok105.codex.hero.pandora.registry;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.damage.DamageSources;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Pandora's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class PandoraDamageTypes {
	public static final ResourceKey<DamageType> SPACE_CRUSH = key("space_crush");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("space_crush", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private PandoraDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource spaceCrush(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, SPACE_CRUSH, attacker);
	}
}
