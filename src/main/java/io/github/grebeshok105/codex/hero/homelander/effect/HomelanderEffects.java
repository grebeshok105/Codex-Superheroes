package io.github.grebeshok105.codex.hero.homelander.effect;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Homelander's madness pair — registered when {@link HomelanderModule} boots. */
public final class HomelanderEffects {
	public static final Holder<MobEffect> MADNESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("madness"),
			new HomelanderMadnessMobEffect(MobEffectCategory.HARMFUL, 0xFF1F2D)
	);

	public static final Holder<MobEffect> MADNESS_AFTERMATH = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("madness_aftermath"),
			new HomelanderMadnessAftermathMobEffect(MobEffectCategory.NEUTRAL, 0xFFE680)
	);

	private HomelanderEffects() {
	}

	public static void init() {
	}

	public static boolean isMadness(net.minecraft.world.entity.LivingEntity entity) {
		return entity != null && entity.hasEffect(MADNESS);
	}

	public static boolean isAftermath(net.minecraft.world.entity.LivingEntity entity) {
		return entity != null && entity.hasEffect(MADNESS_AFTERMATH);
	}
}
