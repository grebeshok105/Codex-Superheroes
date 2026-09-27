package io.github.grebeshok105.codex.hero.doomsday.registry;

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
 * Doomsday's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package. All five sit in {@code BYPASSES_COOLDOWN} —
 * ability damage must not be cut by i-frames (audit B20).
 */
public final class DoomsdayDamageTypes {
	public static final ResourceKey<DamageType> DOOMSDAY_SMASH = key("doomsday_smash");
	public static final ResourceKey<DamageType> DOOMSDAY_ROAR = key("doomsday_roar");
	public static final ResourceKey<DamageType> DOOMSDAY_BONE_SPIKE = key("doomsday_bone_spike");
	public static final ResourceKey<DamageType> DOOMSDAY_CHARGE_TACKLE = key("doomsday_charge_tackle");
	public static final ResourceKey<DamageType> DOOMSDAY_DOOM_GRIP = key("doomsday_doom_grip");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("doomsday_smash", DamageScaling.NEVER, 0.0F, DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("doomsday_roar", DamageScaling.NEVER, 0.0F, DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("doomsday_bone_spike", DamageScaling.NEVER, 0.0F, DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("doomsday_charge_tackle", DamageScaling.NEVER, 0.0F, DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("doomsday_doom_grip", DamageScaling.NEVER, 0.0F, DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private DoomsdayDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource doomsdaySmash(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, DOOMSDAY_SMASH, attacker);
	}

	public static DamageSource doomsdayRoar(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, DOOMSDAY_ROAR, attacker);
	}

	public static DamageSource doomsdayBoneSpike(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, DOOMSDAY_BONE_SPIKE, attacker);
	}

	public static DamageSource doomsdayChargeTackle(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, DOOMSDAY_CHARGE_TACKLE, attacker);
	}

	public static DamageSource doomsdayDoomGrip(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, DOOMSDAY_DOOM_GRIP, attacker);
	}
}
