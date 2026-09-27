package io.github.grebeshok105.codex.hero.goku.registry;

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
 * Goku's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class GokuDamageTypes {
	public static final ResourceKey<DamageType> GOKU_KAMEHAMEHA = key("goku_kamehameha");
	public static final ResourceKey<DamageType> GOKU_INSTANT_STRIKE = key("goku_instant_strike");
	public static final ResourceKey<DamageType> GOKU_SPIRIT_BOMB = key("goku_spirit_bomb");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("goku_kamehameha", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_COOLDOWN, ModDamageTypes.BEAM),
			DamageTypeSpec.of("goku_instant_strike", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("goku_spirit_bomb", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private GokuDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource kamehameha(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, GOKU_KAMEHAMEHA, attacker);
	}

	public static DamageSource instantStrike(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, GOKU_INSTANT_STRIKE, attacker);
	}

	public static DamageSource spiritBomb(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, GOKU_SPIRIT_BOMB, attacker);
	}
}
