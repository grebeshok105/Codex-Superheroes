package com.example.superheroes.damage;

import com.example.superheroes.ModId;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class ModDamageTypes {
	public static final ResourceKey<DamageType> EYE_LASER = key("eye_laser");
	public static final ResourceKey<DamageType> REPULSOR = key("repulsor");
	public static final ResourceKey<DamageType> UNIBEAM = key("unibeam");
	public static final ResourceKey<DamageType> COUNTER_STRIKE = key("counter_strike");
	public static final ResourceKey<DamageType> LION_ROAR = key("lion_roar");
	public static final ResourceKey<DamageType> DOOMSDAY_SMASH = key("doomsday_smash");
	public static final ResourceKey<DamageType> DOOMSDAY_ROAR = key("doomsday_roar");
	public static final ResourceKey<DamageType> DOOMSDAY_BONE_SPIKE = key("doomsday_bone_spike");
	public static final ResourceKey<DamageType> DOOMSDAY_CHARGE_TACKLE = key("doomsday_charge_tackle");
	public static final ResourceKey<DamageType> DOOMSDAY_DOOM_GRIP = key("doomsday_doom_grip");
	public static final ResourceKey<DamageType> SHADOW_ATTACK = key("shadow_attack");
	public static final ResourceKey<DamageType> GOKU_KAMEHAMEHA = key("goku_kamehameha");
	public static final ResourceKey<DamageType> GOKU_INSTANT_STRIKE = key("goku_instant_strike");
	public static final ResourceKey<DamageType> GOKU_SPIRIT_BOMB = key("goku_spirit_bomb");
	public static final ResourceKey<DamageType> NARUTO_RASENGAN = key("naruto_rasengan");
	public static final ResourceKey<DamageType> NARUTO_RASENSHURIKEN = key("naruto_rasenshuriken");
	public static final ResourceKey<DamageType> NARUTO_BIJUUDAMA = key("naruto_bijuudama");
	public static final ResourceKey<DamageType> KRATOS_BLADE = key("kratos_blade");
	public static final ResourceKey<DamageType> KRATOS_LEVIATHAN = key("kratos_leviathan");
	public static final ResourceKey<DamageType> LOKI_CHAOS = key("loki_chaos");
	public static final ResourceKey<DamageType> THANOS_SNAP = key("thanos_snap");
	public static final ResourceKey<DamageType> THANOS_COSMIC_SLAM = key("thanos_cosmic_slam");
	public static final ResourceKey<DamageType> THANOS_MIND_PULSE = key("thanos_mind_pulse");
	public static final ResourceKey<DamageType> THANOS_REALITY_TEAR = key("thanos_reality_tear");
	public static final ResourceKey<DamageType> CAP_SHIELD_THROW = key("cap_shield_throw");
	public static final ResourceKey<DamageType> CAP_SHIELD_SLAM = key("cap_shield_slam");
	public static final ResourceKey<DamageType> HOMELANDER_EYE_LASER = key("homelander_eye_laser");
	public static final ResourceKey<DamageType> HOMELANDER_HEAT_VISION = key("homelander_heat_vision");
	public static final ResourceKey<DamageType> HOMELANDER_HAND_CLAP = key("homelander_hand_clap");
	public static final ResourceKey<DamageType> HOMELANDER_SONIC_SLAM = key("homelander_sonic_slam");
	public static final ResourceKey<DamageType> HOMELANDER_SHOCKWAVE_DIVE = key("homelander_shockwave_dive");
	public static final ResourceKey<DamageType> HOMELANDER_LIGHTNING_CALL = key("homelander_lightning_call");
	public static final ResourceKey<DamageType> HOMELANDER_ROAR_BOSS = key("homelander_roar_boss");
	public static final ResourceKey<DamageType> HOMELANDER_MELEE = key("homelander_melee");
	public static final ResourceKey<DamageType> SPACE_CRUSH = key("space_crush");

	private ModDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static void bootstrap(BootstrapContext<DamageType> context) {
		context.register(EYE_LASER, new DamageType("eye_laser", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(REPULSOR, new DamageType("repulsor", DamageScaling.NEVER, 0.0F));
		context.register(UNIBEAM, new DamageType("unibeam", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(COUNTER_STRIKE, new DamageType("counter_strike", DamageScaling.NEVER, 0.0F));
		context.register(LION_ROAR, new DamageType("lion_roar", DamageScaling.NEVER, 0.0F));
		context.register(DOOMSDAY_SMASH, new DamageType("doomsday_smash", DamageScaling.NEVER, 0.0F));
		context.register(DOOMSDAY_ROAR, new DamageType("doomsday_roar", DamageScaling.NEVER, 0.0F));
		context.register(DOOMSDAY_BONE_SPIKE, new DamageType("doomsday_bone_spike", DamageScaling.NEVER, 0.0F));
		context.register(DOOMSDAY_CHARGE_TACKLE, new DamageType("doomsday_charge_tackle", DamageScaling.NEVER, 0.0F));
		context.register(DOOMSDAY_DOOM_GRIP, new DamageType("doomsday_doom_grip", DamageScaling.NEVER, 0.0F));
		context.register(SHADOW_ATTACK, new DamageType("shadow_attack", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F));
		context.register(GOKU_KAMEHAMEHA, new DamageType("goku_kamehameha", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(GOKU_INSTANT_STRIKE, new DamageType("goku_instant_strike", DamageScaling.NEVER, 0.0F));
		context.register(GOKU_SPIRIT_BOMB, new DamageType("goku_spirit_bomb", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(NARUTO_RASENGAN, new DamageType("naruto_rasengan", DamageScaling.NEVER, 0.0F));
		context.register(NARUTO_RASENSHURIKEN, new DamageType("naruto_rasenshuriken", DamageScaling.NEVER, 0.0F));
		context.register(NARUTO_BIJUUDAMA, new DamageType("naruto_bijuudama", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(KRATOS_BLADE, new DamageType("kratos_blade", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(KRATOS_LEVIATHAN, new DamageType("kratos_leviathan", DamageScaling.NEVER, 0.0F));
		context.register(LOKI_CHAOS, new DamageType("loki_chaos", DamageScaling.NEVER, 0.0F));
		context.register(THANOS_SNAP, new DamageType("thanos_snap", DamageScaling.NEVER, 0.0F));
		context.register(THANOS_COSMIC_SLAM, new DamageType("thanos_cosmic_slam", DamageScaling.NEVER, 0.0F));
		context.register(THANOS_MIND_PULSE, new DamageType("thanos_mind_pulse", DamageScaling.NEVER, 0.0F));
		context.register(THANOS_REALITY_TEAR, new DamageType("thanos_reality_tear", DamageScaling.NEVER, 0.0F));
		context.register(CAP_SHIELD_THROW, new DamageType("cap_shield_throw", DamageScaling.NEVER, 0.0F));
		context.register(CAP_SHIELD_SLAM, new DamageType("cap_shield_slam", DamageScaling.NEVER, 0.0F));
		context.register(HOMELANDER_EYE_LASER, new DamageType("homelander_eye_laser", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(HOMELANDER_HEAT_VISION, new DamageType("homelander_heat_vision", DamageScaling.NEVER, 0.0F, DamageEffects.BURNING));
		context.register(HOMELANDER_HAND_CLAP, new DamageType("homelander_hand_clap", DamageScaling.NEVER, 0.0F));
		context.register(HOMELANDER_SONIC_SLAM, new DamageType("homelander_sonic_slam", DamageScaling.NEVER, 0.0F));
		context.register(HOMELANDER_SHOCKWAVE_DIVE, new DamageType("homelander_shockwave_dive", DamageScaling.NEVER, 0.0F));
		context.register(HOMELANDER_LIGHTNING_CALL, new DamageType("homelander_lightning_call", DamageScaling.NEVER, 0.0F));
		context.register(HOMELANDER_ROAR_BOSS, new DamageType("homelander_roar_boss", DamageScaling.NEVER, 0.0F));
		context.register(HOMELANDER_MELEE, new DamageType("homelander_melee", DamageScaling.NEVER, 0.0F));
		context.register(SPACE_CRUSH, new DamageType("space_crush", DamageScaling.NEVER, 0.0F));
	}

	public static DamageSource eyeLaser(ServerLevel level, Entity attacker) {
		return source(level, EYE_LASER, attacker);
	}

	public static DamageSource repulsor(ServerLevel level, Entity attacker) {
		return source(level, REPULSOR, attacker);
	}

	public static DamageSource unibeam(ServerLevel level, Entity attacker) {
		return source(level, UNIBEAM, attacker);
	}

	public static DamageSource counterStrike(ServerLevel level, Entity attacker) {
		return source(level, COUNTER_STRIKE, attacker);
	}

	public static DamageSource lionRoar(ServerLevel level, Entity attacker) {
		return source(level, LION_ROAR, attacker);
	}

	public static DamageSource doomsdaySmash(ServerLevel level, Entity attacker) {
		return source(level, DOOMSDAY_SMASH, attacker);
	}

	public static DamageSource doomsdayRoar(ServerLevel level, Entity attacker) {
		return source(level, DOOMSDAY_ROAR, attacker);
	}

	public static DamageSource doomsdayBoneSpike(ServerLevel level, Entity attacker) {
		return source(level, DOOMSDAY_BONE_SPIKE, attacker);
	}

	public static DamageSource doomsdayChargeTackle(ServerLevel level, Entity attacker) {
		return source(level, DOOMSDAY_CHARGE_TACKLE, attacker);
	}

	public static DamageSource doomsdayDoomGrip(ServerLevel level, Entity attacker) {
		return source(level, DOOMSDAY_DOOM_GRIP, attacker);
	}

	public static DamageSource shadowAttack(ServerLevel level, Entity attacker) {
		return source(level, SHADOW_ATTACK, attacker);
	}

	public static DamageSource gokuKamehameha(ServerLevel level, Entity attacker) {
		return source(level, GOKU_KAMEHAMEHA, attacker);
	}

	public static DamageSource gokuInstantStrike(ServerLevel level, Entity attacker) {
		return source(level, GOKU_INSTANT_STRIKE, attacker);
	}

	public static DamageSource gokuSpiritBomb(ServerLevel level, Entity attacker) {
		return source(level, GOKU_SPIRIT_BOMB, attacker);
	}

	public static DamageSource narutoRasengan(ServerLevel level, Entity attacker) {
		return source(level, NARUTO_RASENGAN, attacker);
	}

	public static DamageSource narutoRasenshuriken(ServerLevel level, Entity attacker) {
		return source(level, NARUTO_RASENSHURIKEN, attacker);
	}

	public static DamageSource narutoBijuudama(ServerLevel level, Entity attacker) {
		return source(level, NARUTO_BIJUUDAMA, attacker);
	}

	public static DamageSource kratosBlade(ServerLevel level, Entity attacker) {
		return source(level, KRATOS_BLADE, attacker);
	}

	public static DamageSource kratosLeviathan(ServerLevel level, Entity attacker) {
		return source(level, KRATOS_LEVIATHAN, attacker);
	}

	public static DamageSource lokiChaos(ServerLevel level, Entity attacker) {
		return source(level, LOKI_CHAOS, attacker);
	}

	public static DamageSource thanosSnap(ServerLevel level, Entity attacker) {
		return source(level, THANOS_SNAP, attacker);
	}

	public static DamageSource thanosCosmicSlam(ServerLevel level, Entity attacker) {
		return source(level, THANOS_COSMIC_SLAM, attacker);
	}

	public static DamageSource thanosMindPulse(ServerLevel level, Entity attacker) {
		return source(level, THANOS_MIND_PULSE, attacker);
	}

	public static DamageSource thanosRealityTear(ServerLevel level, Entity attacker) {
		return source(level, THANOS_REALITY_TEAR, attacker);
	}

	public static DamageSource capShieldThrow(ServerLevel level, Entity attacker) {
		return source(level, CAP_SHIELD_THROW, attacker);
	}

	public static DamageSource capShieldSlam(ServerLevel level, Entity attacker) {
		return source(level, CAP_SHIELD_SLAM, attacker);
	}

	public static DamageSource homelanderEyeLaser(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_EYE_LASER, attacker);
	}

	public static DamageSource homelanderHeatVision(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_HEAT_VISION, attacker);
	}

	public static DamageSource homelanderHandClap(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_HAND_CLAP, attacker);
	}

	public static DamageSource homelanderSonicSlam(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_SONIC_SLAM, attacker);
	}

	public static DamageSource homelanderShockwaveDive(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_SHOCKWAVE_DIVE, attacker);
	}

	public static DamageSource homelanderLightningCall(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_LIGHTNING_CALL, attacker);
	}

	public static DamageSource homelanderRoarBoss(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_ROAR_BOSS, attacker);
	}

	public static DamageSource homelanderMelee(ServerLevel level, Entity attacker) {
		return source(level, HOMELANDER_MELEE, attacker);
	}

	public static DamageSource spaceCrush(ServerLevel level, Entity attacker) {
		return source(level, SPACE_CRUSH, attacker);
	}

	private static DamageSource source(ServerLevel level, ResourceKey<DamageType> key, Entity attacker) {
		Holder<DamageType> holder = level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(key);
		return new DamageSource(holder, attacker, attacker);
	}
}
