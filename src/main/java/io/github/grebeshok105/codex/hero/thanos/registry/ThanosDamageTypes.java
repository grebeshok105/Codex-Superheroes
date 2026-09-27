package io.github.grebeshok105.codex.hero.thanos.registry;

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
 * Thanos's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class ThanosDamageTypes {
	public static final ResourceKey<DamageType> THANOS_SNAP = key("thanos_snap");
	public static final ResourceKey<DamageType> THANOS_COSMIC_SLAM = key("thanos_cosmic_slam");
	public static final ResourceKey<DamageType> THANOS_MIND_PULSE = key("thanos_mind_pulse");
	public static final ResourceKey<DamageType> THANOS_REALITY_TEAR = key("thanos_reality_tear");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("thanos_snap", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("thanos_cosmic_slam", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("thanos_mind_pulse", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("thanos_reality_tear", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN)
	);

	private ThanosDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource thanosSnap(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, THANOS_SNAP, attacker);
	}

	public static DamageSource thanosCosmicSlam(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, THANOS_COSMIC_SLAM, attacker);
	}

	public static DamageSource thanosMindPulse(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, THANOS_MIND_PULSE, attacker);
	}

	public static DamageSource thanosRealityTear(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, THANOS_REALITY_TEAR, attacker);
	}
}
