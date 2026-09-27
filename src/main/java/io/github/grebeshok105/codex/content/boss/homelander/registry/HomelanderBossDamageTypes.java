package io.github.grebeshok105.codex.content.boss.homelander.registry;

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
 * The boss's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package. {@code homelander_melee} deliberately stays out
 * of {@code bypasses_cooldown} — melee follows vanilla i-frame rules.
 */
public final class HomelanderBossDamageTypes {
	public static final ResourceKey<DamageType> HOMELANDER_EYE_LASER = key("homelander_eye_laser");
	public static final ResourceKey<DamageType> HOMELANDER_HEAT_VISION = key("homelander_heat_vision");
	public static final ResourceKey<DamageType> HOMELANDER_HAND_CLAP = key("homelander_hand_clap");
	public static final ResourceKey<DamageType> HOMELANDER_SONIC_SLAM = key("homelander_sonic_slam");
	public static final ResourceKey<DamageType> HOMELANDER_SHOCKWAVE_DIVE = key("homelander_shockwave_dive");
	public static final ResourceKey<DamageType> HOMELANDER_LIGHTNING_CALL = key("homelander_lightning_call");
	public static final ResourceKey<DamageType> HOMELANDER_ROAR_BOSS = key("homelander_roar_boss");
	public static final ResourceKey<DamageType> HOMELANDER_MELEE = key("homelander_melee");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("homelander_eye_laser", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_COOLDOWN, ModDamageTypes.BEAM),
			DamageTypeSpec.of("homelander_heat_vision", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING,
					DamageTypeTags.BYPASSES_COOLDOWN, ModDamageTypes.BEAM),
			DamageTypeSpec.of("homelander_hand_clap", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("homelander_sonic_slam", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("homelander_shockwave_dive", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("homelander_lightning_call", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("homelander_roar_boss", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("homelander_melee", DamageScaling.NEVER, 0.0F)
	);

	private HomelanderBossDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource eyeLaser(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_EYE_LASER, attacker);
	}

	public static DamageSource heatVision(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_HEAT_VISION, attacker);
	}

	public static DamageSource handClap(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_HAND_CLAP, attacker);
	}

	public static DamageSource sonicSlam(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_SONIC_SLAM, attacker);
	}

	public static DamageSource shockwaveDive(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_SHOCKWAVE_DIVE, attacker);
	}

	public static DamageSource lightningCall(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_LIGHTNING_CALL, attacker);
	}

	public static DamageSource roarBoss(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_ROAR_BOSS, attacker);
	}

	public static DamageSource melee(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, HOMELANDER_MELEE, attacker);
	}
}
