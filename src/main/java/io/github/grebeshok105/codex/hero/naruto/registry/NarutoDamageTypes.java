package io.github.grebeshok105.codex.hero.naruto.registry;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.damage.DamageSources;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
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
 * Naruto's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class NarutoDamageTypes {
	public static final ResourceKey<DamageType> NARUTO_RASENGAN = key("naruto_rasengan");
	public static final ResourceKey<DamageType> NARUTO_RASENSHURIKEN = key("naruto_rasenshuriken");
	public static final ResourceKey<DamageType> NARUTO_BIJUUDAMA = key("naruto_bijuudama");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("naruto_rasengan", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("naruto_rasenshuriken", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("naruto_bijuudama", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private NarutoDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource rasengan(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, NARUTO_RASENGAN, attacker);
	}

	public static DamageSource rasenshuriken(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, NARUTO_RASENSHURIKEN, attacker);
	}

	public static DamageSource bijuudama(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, NARUTO_BIJUUDAMA, attacker);
	}
}
