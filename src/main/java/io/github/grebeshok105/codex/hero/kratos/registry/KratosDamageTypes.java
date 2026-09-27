package io.github.grebeshok105.codex.hero.kratos.registry;

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
 * Kratos's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class KratosDamageTypes {
	public static final ResourceKey<DamageType> KRATOS_BLADE = key("kratos_blade");
	public static final ResourceKey<DamageType> KRATOS_LEVIATHAN = key("kratos_leviathan");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("kratos_blade", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("kratos_leviathan", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private KratosDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource blade(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, KRATOS_BLADE, attacker);
	}

	public static DamageSource leviathan(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, KRATOS_LEVIATHAN, attacker);
	}
}
