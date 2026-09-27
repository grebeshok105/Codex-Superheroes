package io.github.grebeshok105.codex.hero.captainamerica.registry;

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
 * Captain America's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes}
 * used to own; the module exposes them through {@code damageTypes()} so datagen and tag
 * providers enumerate them without naming this package.
 */
public final class CaptainAmericaDamageTypes {
	public static final ResourceKey<DamageType> CAP_SHIELD_THROW = key("cap_shield_throw");
	public static final ResourceKey<DamageType> CAP_SHIELD_SLAM = key("cap_shield_slam");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("cap_shield_throw", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("cap_shield_slam", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private CaptainAmericaDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource capShieldThrow(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, CAP_SHIELD_THROW, attacker);
	}

	public static DamageSource capShieldSlam(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, CAP_SHIELD_SLAM, attacker);
	}
}
