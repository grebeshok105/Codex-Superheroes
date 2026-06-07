package com.example.superheroes.particle;

import com.example.superheroes.ModId;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
	public static final SimpleParticleType TRANSFORM_SPARK = register("transform_spark", FabricParticleTypes.simple());
	public static final SimpleParticleType LASER_SPARK = register("laser_spark", FabricParticleTypes.simple());
	public static final SimpleParticleType REPULSOR_SPARK = register("repulsor_spark", FabricParticleTypes.simple());
	public static final SimpleParticleType UNIBEAM_SPARK = register("unibeam_spark", FabricParticleTypes.simple());

	public static final SimpleParticleType GOKU_KI_AURA = register("goku_ki_aura", FabricParticleTypes.simple());
	public static final SimpleParticleType GOKU_KAMEHAMEHA_CORE = register("goku_kamehameha_core", FabricParticleTypes.simple());
	public static final SimpleParticleType GOKU_KAMEHAMEHA_TRAIL = register("goku_kamehameha_trail", FabricParticleTypes.simple());

	public static final SimpleParticleType NARUTO_RASENGAN_SWIRL = register("naruto_rasengan_swirl", FabricParticleTypes.simple());
	public static final SimpleParticleType NARUTO_CLONE_POOF = register("naruto_clone_poof", FabricParticleTypes.simple());
	public static final SimpleParticleType NARUTO_KAWARIMI_SMOKE = register("naruto_kawarimi_smoke", FabricParticleTypes.simple());

	public static final SimpleParticleType CAP_SHIELD_TRAIL = register("cap_shield_trail", FabricParticleTypes.simple());
	public static final SimpleParticleType CAP_SHIELD_SLAM_BURST = register("cap_shield_slam_burst", FabricParticleTypes.simple());

	public static final SimpleParticleType WHITE_BOOM = register("white_boom", FabricParticleTypes.simple());
	public static final SimpleParticleType SWORD_EXPLOSION = register("sword_explosion", FabricParticleTypes.simple());
	public static final SimpleParticleType SPARKS = register("sparks", FabricParticleTypes.simple());
	public static final SimpleParticleType DARK_STAR = register("dark_star", FabricParticleTypes.simple());
	public static final SimpleParticleType PURPLE_FLAME = register("purple_flame", FabricParticleTypes.simple());
	public static final SimpleParticleType BLACK_FLAME = register("black_flame", FabricParticleTypes.simple());
	public static final SimpleParticleType DAZZLING = register("dazzling", FabricParticleTypes.simple());
	public static final SimpleParticleType SUN_PARTICLE = register("sun_particle", FabricParticleTypes.simple());
	public static final SimpleParticleType SOUL_SPARK = register("soul_spark", FabricParticleTypes.simple());
	public static final SimpleParticleType NIGHTFALL = register("nightfall", FabricParticleTypes.simple());
	public static final SimpleParticleType CHAOS_ORB = register("chaos_orb", FabricParticleTypes.simple());
	public static final SimpleParticleType KRATOS_HAND_BURST_1 = register("kratos_hand_burst_1", FabricParticleTypes.simple());
	public static final SimpleParticleType KRATOS_HAND_BURST_2 = register("kratos_hand_burst_2", FabricParticleTypes.simple());
	public static final SimpleParticleType KRATOS_HAND_BURST_3 = register("kratos_hand_burst_3", FabricParticleTypes.simple());

	public static final SimpleParticleType ANOMALY_SLICE = register("anomaly_slice", FabricParticleTypes.simple());
	public static final SimpleParticleType JIWALD_EFFECT = register("jiwald_effect", FabricParticleTypes.simple());
	public static final SimpleParticleType FULA_PARTICLE = register("fula_particle", FabricParticleTypes.simple());
	public static final SimpleParticleType SHAMAK = register("shamak", FabricParticleTypes.simple());
	public static final SimpleParticleType BLUE_FLAME = register("blue_flame", FabricParticleTypes.simple());
	public static final SimpleParticleType MOONVEIL = register("moonveil", FabricParticleTypes.simple());

	private ModParticles() {
	}

	private static SimpleParticleType register(String name, SimpleParticleType type) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModId.of(name), type);
	}

	public static void init() {
	}
}
