package io.github.grebeshok105.codex.damage;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class ModDamageTypes {
	public static final ResourceKey<DamageType> LOKI_CHAOS = key("loki_chaos");

	/** Beam-type damage — read by hero code instead of a hardcoded key list (populated by datagen). */
	public static final TagKey<DamageType> BEAM = TagKey.create(Registries.DAMAGE_TYPE, ModId.of("beam"));

	private ModDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static void bootstrap(BootstrapContext<DamageType> context) {
		context.register(LOKI_CHAOS, new DamageType("loki_chaos", DamageScaling.NEVER, 0.0F));
	}

	public static DamageSource lokiChaos(ServerLevel level, Entity attacker) {
		return source(level, LOKI_CHAOS, attacker);
	}

	private static DamageSource source(ServerLevel level, ResourceKey<DamageType> key, Entity attacker) {
		Holder<DamageType> holder = level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(key);
		return new DamageSource(holder, attacker, attacker);
	}
}
