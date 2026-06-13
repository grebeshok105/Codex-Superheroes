package com.example.superheroes.effect;

import com.example.superheroes.ModId;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	public static final Holder<MobEffect> MADNESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("madness"),
			new MadnessMobEffect(MobEffectCategory.HARMFUL, 0xFF1F2D)
	);

	public static final Holder<MobEffect> MADNESS_AFTERMATH = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("madness_aftermath"),
			new MadnessAftermathMobEffect(MobEffectCategory.NEUTRAL, 0xFFE680)
	);

	public static final Holder<MobEffect> SUPERHERO_WEAKNESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("superhero_weakness"),
			new SuperheroWeaknessEffect(MobEffectCategory.HARMFUL, 0xFF7FFF30)
	);

	public static final Holder<MobEffect> SNAPPED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("snapped"),
			new SnappedMobEffect(MobEffectCategory.HARMFUL, 0xFF6A0DAD)
	);

	public static final Holder<MobEffect> DISABLED_ABILITIES = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("disabled_abilities"),
			new DisabledAbilitiesMobEffect(MobEffectCategory.HARMFUL, 0xFF4A148C)
	);

	public static final Holder<MobEffect> VANITY_STRIPPED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("vanity_stripped"),
			new VanityStrippedMobEffect(MobEffectCategory.HARMFUL, 0xFF2A0A3A)
	);

	public static final Holder<MobEffect> HEAL_BLOCK = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("heal_block"),
			new HealBlockMobEffect(MobEffectCategory.HARMFUL, 0xFF7C0045)
	);

	public static final Holder<MobEffect> BLEEDING = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("bleeding"),
			new BleedingMobEffect(MobEffectCategory.HARMFUL, 0xB3121D)
	);

	private ModEffects() {
	}

	public static void init() {
	}

	public static boolean isMadness(net.minecraft.world.entity.LivingEntity entity) {
		return entity != null && entity.hasEffect(MADNESS);
	}

	public static boolean isAftermath(net.minecraft.world.entity.LivingEntity entity) {
		return entity != null && entity.hasEffect(MADNESS_AFTERMATH);
	}

	public static boolean isVanityStripped(net.minecraft.world.entity.LivingEntity entity) {
		return entity != null && entity.hasEffect(VANITY_STRIPPED);
	}
}
