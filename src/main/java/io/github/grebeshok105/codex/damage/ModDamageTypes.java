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
	public static final ResourceKey<DamageType> REPULSOR = key("repulsor");
	public static final ResourceKey<DamageType> UNIBEAM = key("unibeam");
	public static final ResourceKey<DamageType> SHADOW_ATTACK = key("shadow_attack");
	public static final ResourceKey<DamageType> LOKI_CHAOS = key("loki_chaos");
	public static final ResourceKey<DamageType> CAP_SHIELD_THROW = key("cap_shield_throw");
	public static final ResourceKey<DamageType> CAP_SHIELD_SLAM = key("cap_shield_slam");

	/** Beam-type damage — read by hero code instead of a hardcoded key list (populated by datagen). */
	public static final TagKey<DamageType> BEAM = TagKey.create(Registries.DAMAGE_TYPE, ModId.of("beam"));

	private ModDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static void bootstrap(BootstrapContext<DamageType> context) {
		context.register(REPULSOR, new DamageType("repulsor", DamageScaling.NEVER, 0.0F));
		context.register(UNIBEAM, new DamageType("unibeam", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(SHADOW_ATTACK, new DamageType("shadow_attack", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F));
		context.register(LOKI_CHAOS, new DamageType("loki_chaos", DamageScaling.NEVER, 0.0F));
		context.register(CAP_SHIELD_THROW, new DamageType("cap_shield_throw", DamageScaling.NEVER, 0.0F));
		context.register(CAP_SHIELD_SLAM, new DamageType("cap_shield_slam", DamageScaling.NEVER, 0.0F));
	}

	public static DamageSource repulsor(ServerLevel level, Entity attacker) {
		return source(level, REPULSOR, attacker);
	}

	public static DamageSource unibeam(ServerLevel level, Entity attacker) {
		return source(level, UNIBEAM, attacker);
	}

	public static DamageSource shadowAttack(ServerLevel level, Entity attacker) {
		return source(level, SHADOW_ATTACK, attacker);
	}

	public static DamageSource lokiChaos(ServerLevel level, Entity attacker) {
		return source(level, LOKI_CHAOS, attacker);
	}

	public static DamageSource capShieldThrow(ServerLevel level, Entity attacker) {
		return source(level, CAP_SHIELD_THROW, attacker);
	}

	public static DamageSource capShieldSlam(ServerLevel level, Entity attacker) {
		return source(level, CAP_SHIELD_SLAM, attacker);
	}

	private static DamageSource source(ServerLevel level, ResourceKey<DamageType> key, Entity attacker) {
		Holder<DamageType> holder = level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(key);
		return new DamageSource(holder, attacker, attacker);
	}
}
